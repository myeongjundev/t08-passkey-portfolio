package dev.myeongjun.passkey.web;

import com.jayway.jsonpath.JsonPath;
import com.webauthn4j.converter.util.ObjectConverter;
import dev.myeongjun.passkey.account.AccountRepository;
import dev.myeongjun.passkey.ceremony.CeremonyKind;
import dev.myeongjun.passkey.ceremony.CeremonyRepository;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.ceremony.CeremonyRejectedException;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.session.SessionSecurityContext;
import dev.myeongjun.passkey.session.SessionAuthentication;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.privatearea.PrivateItemRepository;
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

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationOptionsHttpTests {

    private static final String VALID_BODY = """
            {"displayName":"합성 계정 A","nickname":"노트북 패스키"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CeremonyRepository ceremonyRepository;

    @Autowired
    private CeremonyService ceremonyService;

    @Autowired
    private WebAuthnProperties properties;

    @Autowired private ObjectConverter objectConverter;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasskeyCredentialRepository credentialRepository;
    @Autowired private PrivateItemRepository privateItemRepository;

    @BeforeEach
    void clearCeremonies() {
        ceremonyRepository.deleteAll();
        privateItemRepository.deleteAll();
        credentialRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void returnsRequiredDiscoverableCredentialOptionsAndPersistsTheChallenge() throws Exception {
        MockHttpSession session = bootstrapSession();

        MvcResult first = performValidOptions(session)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.publicKey.rp.id").value("localhost"))
                .andExpect(jsonPath("$.publicKey.user.displayName").value("합성 계정 A"))
                .andExpect(jsonPath("$.publicKey.pubKeyCredParams[0].alg").value(-7))
                .andExpect(jsonPath("$.publicKey.pubKeyCredParams[1].alg").value(-257))
                .andExpect(jsonPath("$.publicKey.authenticatorSelection.residentKey").value("required"))
                .andExpect(jsonPath("$.publicKey.authenticatorSelection.requireResidentKey").value(true))
                .andExpect(jsonPath("$.publicKey.authenticatorSelection.userVerification").value("required"))
                .andExpect(jsonPath("$.publicKey.attestation").value("none"))
                .andExpect(content().string(not(containsString("password"))))
                .andReturn();

        String firstJson = first.getResponse().getContentAsString();
        String firstChallenge = JsonPath.read(firstJson, "$.publicKey.challenge");
        String firstUserHandle = JsonPath.read(firstJson, "$.publicKey.user.id");
        String firstCeremonyId = JsonPath.read(firstJson, "$.ceremonyId");

        assertThat(Base64.getUrlDecoder().decode(firstChallenge)).hasSize(CeremonyService.RANDOM_VALUE_BYTES);
        assertThat(Base64.getUrlDecoder().decode(firstUserHandle)).hasSize(CeremonyService.RANDOM_VALUE_BYTES);
        assertThat(ceremonyRepository.existsById(UUID.fromString(firstCeremonyId))).isTrue();

        MvcResult second = performValidOptions(session).andExpect(status().isOk()).andReturn();
        String secondChallenge = JsonPath.read(second.getResponse().getContentAsString(), "$.publicKey.challenge");

        assertThat(secondChallenge).isNotEqualTo(firstChallenge);
        assertThat(ceremonyRepository.countByOwnerKeyAndKindAndActiveSlot(
                (byte[]) session.getAttribute(SessionSecurityContext.OWNER_KEY_ATTRIBUTE),
                CeremonyKind.CREATE_ACCOUNT,
                "ACTIVE"
        )).isEqualTo(1);
    }

    @Test
    void rejectsMissingContentTypeOriginAndCsrfBeforeCreatingRows() throws Exception {
        MockHttpSession session = bootstrapSession();
        String csrf = csrf(session);

        mockMvc.perform(post("/api/webauthn/register/options").session(session).content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType());

        mockMvc.perform(post("/api/webauthn/register/options")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-CSRF-Token", csrf)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/webauthn/register/options")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "https://attacker.invalid")
                        .header("X-CSRF-Token", csrf)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/webauthn/register/options")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", properties.origin().toString())
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/webauthn/register/options")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", properties.origin().toString())
                        .header("X-CSRF-Token", "wrong-token")
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        assertThat(ceremonyRepository.count()).isZero();
    }

    @Test
    void rejectsInvalidLabelsWithoutCreatingCeremony() throws Exception {
        MockHttpSession session = bootstrapSession();

        mockMvc.perform(post("/api/webauthn/register/options")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", properties.origin().toString())
                        .header("X-CSRF-Token", csrf(session))
                        .content("{\"displayName\":\" \",\"nickname\":\"패스키\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"invalid_request\"}"));

        assertThat(ceremonyRepository.count()).isZero();
    }

    @Test
    void finishesVerifiedRegistrationRotatesSessionAndOpensPrivateArea() throws Exception {
        MockHttpSession anonymousSession = bootstrapSession();
        String anonymousSessionId = anonymousSession.getId();
        String oldCsrf = csrf(anonymousSession);
        MvcResult optionsResult = performValidOptions(anonymousSession)
                .andExpect(status().isOk())
                .andReturn();
        String optionsJson = optionsResult.getResponse().getContentAsString();
        String challenge = JsonPath.read(optionsJson, "$.publicKey.challenge");
        String ceremonyId = JsonPath.read(optionsJson, "$.ceremonyId");
        String credentialJson = RegistrationCredentialFixture.create(
                objectConverter,
                Base64.getUrlDecoder().decode(challenge),
                properties.rpId(),
                properties.origin().toString()
        );
        String finishBody = "{\"ceremonyId\":\"" + ceremonyId + "\",\"credential\":" + credentialJson + "}";

        MvcResult finishResult = mockMvc.perform(post("/api/webauthn/register/finish")
                        .session(anonymousSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", properties.origin().toString())
                        .header("X-CSRF-Token", oldCsrf)
                        .content(finishBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.redirectTo").value("/private"))
                .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession) finishResult.getRequest().getSession(false);
        assertThat(authenticatedSession.getId()).isNotEqualTo(anonymousSessionId);
        assertThat(authenticatedSession.getAttribute(SessionAuthentication.PRINCIPAL_ATTRIBUTE)).isNotNull();
        assertThat(csrf(authenticatedSession)).isNotEqualTo(oldCsrf);
        assertThat(accountRepository.count()).isOne();
        assertThat(credentialRepository.count()).isOne();
        assertThat(privateItemRepository.count()).isEqualTo(3);

        mockMvc.perform(get("/api/private-items").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void cancellationConsumesAttemptWithoutCreatingAccountOrCredential() throws Exception {
        MockHttpSession session = bootstrapSession();
        MvcResult optionsResult = performValidOptions(session).andExpect(status().isOk()).andReturn();
        String ceremonyId = JsonPath.read(optionsResult.getResponse().getContentAsString(), "$.ceremonyId");

        mockMvc.perform(post("/api/webauthn/register/cancel")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", properties.origin().toString())
                        .header("X-CSRF-Token", csrf(session))
                        .content("{\"ceremonyId\":\"" + ceremonyId + "\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        assertThat(accountRepository.count()).isZero();
        assertThat(credentialRepository.count()).isZero();
        assertThat(privateItemRepository.count()).isZero();
        assertThatThrownBy(() -> ceremonyService.consume(
                UUID.fromString(ceremonyId),
                CeremonyKind.CREATE_ACCOUNT,
                (byte[]) session.getAttribute(SessionSecurityContext.OWNER_KEY_ATTRIBUTE)
        )).isInstanceOf(CeremonyRejectedException.class);
    }

    private MockHttpSession bootstrapSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/access"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(content().string(containsString("name=\"csrf-token\"")))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private org.springframework.test.web.servlet.ResultActions performValidOptions(MockHttpSession session)
            throws Exception {
        return mockMvc.perform(post("/api/webauthn/register/options")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", properties.origin().toString())
                .header("X-CSRF-Token", csrf(session))
                .content(VALID_BODY));
    }

    private String csrf(MockHttpSession session) {
        return (String) session.getAttribute(SessionSecurityContext.CSRF_ATTRIBUTE);
    }
}
