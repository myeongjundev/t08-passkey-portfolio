package dev.myeongjun.passkey.privatearea;

import com.webauthn4j.converter.util.ObjectConverter;
import dev.myeongjun.passkey.account.AccountRegistrationService;
import dev.myeongjun.passkey.account.AccountAuthenticationService;
import dev.myeongjun.passkey.account.AccountRepository;
import dev.myeongjun.passkey.account.RegisteredAccount;
import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyRepository;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.session.SessionAuthentication;
import dev.myeongjun.passkey.session.SessionPrincipal;
import dev.myeongjun.passkey.webauthn.RegistrationCredentialFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CrossAccountIsolationTests {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRegistrationService registrationService;
    @Autowired AccountAuthenticationService authenticationService;
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
    void bothDirectionsAreHiddenAndForeignAccountParameterCannotSelectOwnership() throws Exception {
        Enrolled accountA = enroll("합성 계정 A", (byte) 61);
        Enrolled accountB = enroll("합성 계정 B", (byte) 62);
        MockHttpSession sessionA = authenticate(accountA, (byte) 63);
        MockHttpSession sessionB = authenticate(accountB, (byte) 64);
        List<PrivateItem> itemsA = privateItemRepository.findAllByAccountIdOrderBySortOrder(accountA.account().accountId());
        List<PrivateItem> itemsB = privateItemRepository.findAllByAccountIdOrderBySortOrder(accountB.account().accountId());
        assertThat(itemsA).hasSize(3);
        assertThat(itemsB).hasSize(3);
        assertThat(itemsA).extracting(PrivateItem::body).allMatch(body -> body.contains("합성 계정 A"));
        assertThat(itemsB).extracting(PrivateItem::body).allMatch(body -> body.contains("합성 계정 B"));

        long countABefore = privateItemRepository.count();
        mockMvc.perform(get("/api/private-items/{itemId}", itemsB.getFirst().id()).session(sessionA))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":\"resource_not_found\"}"));
        assertThat(privateItemRepository.count()).isEqualTo(countABefore);

        long countBBefore = privateItemRepository.count();
        mockMvc.perform(get("/api/private-items/{itemId}", itemsA.getFirst().id()).session(sessionB))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":\"resource_not_found\"}"));
        assertThat(privateItemRepository.count()).isEqualTo(countBBefore);

        mockMvc.perform(get("/api/private-items")
                        .queryParam("accountId", accountB.account().accountId().toString())
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].body").value(org.hamcrest.Matchers.containsString("합성 계정 A")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("합성 계정 B 전용"))));
    }

    private Enrolled enroll(String displayName, byte ownerByte) {
        byte[] owner = bytes(ownerByte);
        CeremonyOptions options = ceremonyService.issueCreateAccount(owner, displayName, displayName + " 키");
        var credential = RegistrationCredentialFixture.createBundle(
                objectConverter, options.challenge(), properties.rpId(), properties.origin().toString());
        RegisteredAccount account = registrationService.finish(options.id(), owner, credential.registrationJson());
        return new Enrolled(account, credential);
    }

    private MockHttpSession authenticate(Enrolled enrolled, byte ownerByte) {
        byte[] owner = bytes(ownerByte);
        CeremonyOptions options = ceremonyService.issueAuthentication(owner);
        byte[] userHandle = accountRepository.findById(enrolled.account().accountId()).orElseThrow().userHandle();
        String assertion = enrolled.credential().authenticationJson(
                objectConverter, options.challenge(), properties.rpId(), properties.origin().toString(),
                userHandle, 1, true);
        RegisteredAccount account = authenticationService.finish(options.id(), owner, assertion);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAuthentication.PRINCIPAL_ATTRIBUTE,
                new SessionPrincipal(account.accountId(), account.displayName()));
        return session;
    }

    private record Enrolled(
            RegisteredAccount account,
            RegistrationCredentialFixture.CredentialBundle credential
    ) {
    }

    private byte[] bytes(byte value) {
        byte[] result = new byte[32];
        Arrays.fill(result, value);
        return result;
    }
}
