package dev.myeongjun.passkey.web;

import com.jayway.jsonpath.JsonPath;
import com.webauthn4j.converter.util.ObjectConverter;
import dev.myeongjun.passkey.account.AccountRegistrationService;
import dev.myeongjun.passkey.account.AccountRepository;
import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyRepository;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.privatearea.PrivateItemRepository;
import dev.myeongjun.passkey.session.SessionAuthentication;
import dev.myeongjun.passkey.session.SessionPrincipal;
import dev.myeongjun.passkey.session.SessionSecurityContext;
import dev.myeongjun.passkey.webauthn.RegistrationCredentialFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Base64;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasskeyManagementHttpTests {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRegistrationService registrationService;
    @Autowired CeremonyService ceremonyService;
    @Autowired CeremonyRepository ceremonyRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired PasskeyCredentialRepository credentialRepository;
    @Autowired PrivateItemRepository privateItemRepository;
    @Autowired ObjectConverter objectConverter;
    @Autowired WebAuthnProperties properties;

    @BeforeEach
    void cleanData() {
        ceremonyRepository.deleteAll();
        privateItemRepository.deleteAll();
        credentialRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void authenticatedApiAddsListsAndDeletesButKeepsFinalPasskey() throws Exception {
        var registered = enroll();
        MockHttpSession session = session(registered.accountId());

        mockMvc.perform(get("/api/passkeys")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/private").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("주 기기")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("마지막 패스키는 삭제할 수 없습니다")));

        MvcResult options = postJson("/api/passkeys/options", session, "{\"nickname\":\"예비 키\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey.excludeCredentials", hasSize(1)))
                .andReturn();
        String optionsJson = options.getResponse().getContentAsString();
        String ceremonyId = JsonPath.read(optionsJson, "$.ceremonyId");
        byte[] challenge = Base64.getUrlDecoder().decode((String) JsonPath.read(optionsJson, "$.publicKey.challenge"));
        var second = RegistrationCredentialFixture.createBundle(
                objectConverter, challenge, properties.rpId(), properties.origin().toString());
        String finishBody = "{\"ceremonyId\":\"" + ceremonyId + "\",\"credential\":"
                + second.registrationJson() + "}";
        postJson("/api/passkeys/finish", session, finishBody).andExpect(status().isCreated());

        MvcResult list = mockMvc.perform(get("/api/passkeys").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nickname").value("주 기기"))
                .andExpect(jsonPath("$[1].nickname").value("예비 키"))
                .andReturn();
        String firstId = JsonPath.read(list.getResponse().getContentAsString(), "$[0].id");
        String secondId = JsonPath.read(list.getResponse().getContentAsString(), "$[1].id");

        deleteJson("/api/passkeys/" + firstId, session)
                .andExpect(status().isNoContent());
        deleteJson("/api/passkeys/" + secondId, session)
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"final_passkey_required\"}"));
        mockMvc.perform(get("/api/passkeys").session(session))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nickname").value("예비 키"));
    }

    private SessionPrincipal enroll() {
        byte[] owner = bytes((byte) 51);
        CeremonyOptions options = ceremonyService.issueCreateAccount(owner, "합성 관리 계정", "주 기기");
        var bundle = RegistrationCredentialFixture.createBundle(
                objectConverter, options.challenge(), properties.rpId(), properties.origin().toString());
        var result = registrationService.finish(options.id(), owner, bundle.registrationJson());
        return new SessionPrincipal(result.accountId(), result.displayName());
    }

    private MockHttpSession session(java.util.UUID accountId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthentication.PRINCIPAL_ATTRIBUTE, new SessionPrincipal(accountId, "합성 관리 계정"));
        session.setAttribute(SessionSecurityContext.OWNER_KEY_ATTRIBUTE, bytes((byte) 52));
        session.setAttribute(SessionSecurityContext.CSRF_ATTRIBUTE, "synthetic-csrf-token");
        return session;
    }

    private org.springframework.test.web.servlet.ResultActions postJson(
            String path, MockHttpSession session, String body) throws Exception {
        return mockMvc.perform(post(path).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", properties.origin().toString())
                .header("X-CSRF-Token", session.getAttribute(SessionSecurityContext.CSRF_ATTRIBUTE))
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions deleteJson(
            String path, MockHttpSession session) throws Exception {
        return mockMvc.perform(delete(path).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", properties.origin().toString())
                .header("X-CSRF-Token", session.getAttribute(SessionSecurityContext.CSRF_ATTRIBUTE))
                .content("{}"));
    }

    private byte[] bytes(byte value) {
        byte[] result = new byte[32];
        Arrays.fill(result, value);
        return result;
    }
}
