package dev.myeongjun.passkey.web;

import com.jayway.jsonpath.JsonPath;
import com.webauthn4j.converter.util.ObjectConverter;
import dev.myeongjun.passkey.account.Account;
import dev.myeongjun.passkey.account.AccountRegistrationService;
import dev.myeongjun.passkey.account.AccountRepository;
import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyRepository;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.privatearea.PrivateItemRepository;
import dev.myeongjun.passkey.session.SessionAuthentication;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationHttpTests {

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
    void loginRotatesSessionAndLogoutRevokesPrivateAccessAndReplay() throws Exception {
        Enrolled enrolled = enroll();
        MockHttpSession loginSession = bootstrapSession();
        String oldSessionId = loginSession.getId();
        MvcResult optionsResult = postJson("/api/webauthn/authenticate/options", loginSession, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey.allowCredentials", hasSize(0)))
                .andExpect(jsonPath("$.publicKey.userVerification").value("required"))
                .andReturn();
        String options = optionsResult.getResponse().getContentAsString();
        String ceremonyId = JsonPath.read(options, "$.ceremonyId");
        byte[] challenge = Base64.getUrlDecoder().decode((String) JsonPath.read(options, "$.publicKey.challenge"));
        String assertion = enrolled.bundle().authenticationJson(
                objectConverter, challenge, properties.rpId(), properties.origin().toString(),
                enrolled.account().userHandle(), 1, true);
        String finishBody = "{\"ceremonyId\":\"" + ceremonyId + "\",\"credential\":" + assertion + "}";

        MvcResult finish = postJson("/api/webauthn/authenticate/finish", loginSession, finishBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectTo").value("/private"))
                .andReturn();
        MockHttpSession authenticated = (MockHttpSession) finish.getRequest().getSession(false);
        assertThat(authenticated.getId()).isNotEqualTo(oldSessionId);
        assertThat(authenticated.getAttribute(SessionAuthentication.PRINCIPAL_ATTRIBUTE)).isNotNull();
        mockMvc.perform(get("/api/private-items").session(authenticated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        postJson("/api/session/logout", authenticated, "{}").andExpect(status().isNoContent());
        mockMvc.perform(get("/api/private-items")).andExpect(status().isUnauthorized());

        MockHttpSession replaySession = bootstrapSession();
        postJson("/api/webauthn/authenticate/finish", replaySession, finishBody)
                .andExpect(status().isBadRequest());
    }

    @Test
    void eachAuthenticationOptionsRequestReturnsANewServerHeldChallenge() throws Exception {
        MockHttpSession session = bootstrapSession();
        MvcResult first = postJson("/api/webauthn/authenticate/options", session, "{}")
                .andExpect(status().isOk()).andReturn();
        MvcResult second = postJson("/api/webauthn/authenticate/options", session, "{}")
                .andExpect(status().isOk()).andReturn();

        String firstChallenge = JsonPath.read(first.getResponse().getContentAsString(), "$.publicKey.challenge");
        String secondChallenge = JsonPath.read(second.getResponse().getContentAsString(), "$.publicKey.challenge");
        assertThat(firstChallenge).isNotEqualTo(secondChallenge);
        assertThat(Base64.getUrlDecoder().decode(firstChallenge)).hasSize(32);
        assertThat(Base64.getUrlDecoder().decode(secondChallenge)).hasSize(32);
    }

    private Enrolled enroll() {
        byte[] owner = ownerKey((byte) 31);
        CeremonyOptions registration = ceremonyService.issueCreateAccount(owner, "합성 로그인 계정", "로그인 키");
        var bundle = RegistrationCredentialFixture.createBundle(
                objectConverter, registration.challenge(), properties.rpId(), properties.origin().toString());
        var registered = registrationService.finish(registration.id(), owner, bundle.registrationJson());
        return new Enrolled(accountRepository.findById(registered.accountId()).orElseThrow(), bundle);
    }

    private MockHttpSession bootstrapSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/access")).andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private org.springframework.test.web.servlet.ResultActions postJson(
            String path, MockHttpSession session, String body) throws Exception {
        return mockMvc.perform(post(path)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", properties.origin().toString())
                .header("X-CSRF-Token", session.getAttribute(SessionSecurityContext.CSRF_ATTRIBUTE))
                .content(body));
    }

    private byte[] ownerKey(byte value) {
        byte[] result = new byte[32];
        Arrays.fill(result, value);
        return result;
    }

    private record Enrolled(Account account, RegistrationCredentialFixture.CredentialBundle bundle) {
    }
}
