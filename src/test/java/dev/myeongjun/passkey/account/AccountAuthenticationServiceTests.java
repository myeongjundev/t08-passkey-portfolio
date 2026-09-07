package dev.myeongjun.passkey.account;

import com.webauthn4j.converter.util.ObjectConverter;
import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyRepository;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.privatearea.PrivateItemRepository;
import dev.myeongjun.passkey.webauthn.AuthenticationVerificationException;
import dev.myeongjun.passkey.webauthn.RegistrationCredentialFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AccountAuthenticationServiceTests {

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
    void storedPublicKeyVerifiesSignatureAndUpdatesCounter() {
        Enrolled enrolled = enroll((byte) 21);
        byte[] loginOwner = ownerKey((byte) 22);
        CeremonyOptions login = ceremonyService.issueAuthentication(loginOwner);
        String assertion = enrolled.bundle().authenticationJson(
                objectConverter, login.challenge(), properties.rpId(), properties.origin().toString(),
                enrolled.account().userHandle(), 1, true);

        RegisteredAccount authenticated = authenticationService.finish(login.id(), loginOwner, assertion);

        assertThat(authenticated.accountId()).isEqualTo(enrolled.account().id());
        assertThat(credentialRepository.findAll().getFirst().signCount()).isEqualTo(1);
    }

    @Test
    void badSignatureCreatesNoSessionResultAndBurnsChallenge() {
        Enrolled enrolled = enroll((byte) 23);
        byte[] loginOwner = ownerKey((byte) 24);
        CeremonyOptions login = ceremonyService.issueAuthentication(loginOwner);
        String assertion = enrolled.bundle().authenticationJson(
                objectConverter, login.challenge(), properties.rpId(), properties.origin().toString(),
                enrolled.account().userHandle(), 1, false);

        assertThatThrownBy(() -> authenticationService.finish(login.id(), loginOwner, assertion))
                .isInstanceOf(AuthenticationVerificationException.class);
        assertThat(credentialRepository.findAll().getFirst().signCount()).isZero();
        assertThatThrownBy(() -> authenticationService.finish(login.id(), loginOwner, assertion))
                .isInstanceOf(dev.myeongjun.passkey.ceremony.CeremonyRejectedException.class);
    }

    @Test
    void authenticatorsWithoutCounterSupportMayKeepZero() {
        Enrolled enrolled = enroll((byte) 25);
        byte[] loginOwner = ownerKey((byte) 26);
        CeremonyOptions login = ceremonyService.issueAuthentication(loginOwner);
        String assertion = enrolled.bundle().authenticationJson(
                objectConverter, login.challenge(), properties.rpId(), properties.origin().toString(),
                enrolled.account().userHandle(), 0, true);

        authenticationService.finish(login.id(), loginOwner, assertion);

        assertThat(credentialRepository.findAll().getFirst().signCount()).isZero();
    }

    private Enrolled enroll(byte ownerValue) {
        byte[] owner = ownerKey(ownerValue);
        CeremonyOptions registration = ceremonyService.issueCreateAccount(owner, "합성 로그인 사용자", "테스트 키");
        var bundle = RegistrationCredentialFixture.createBundle(
                objectConverter, registration.challenge(), properties.rpId(), properties.origin().toString());
        RegisteredAccount registered = registrationService.finish(registration.id(), owner, bundle.registrationJson());
        return new Enrolled(accountRepository.findById(registered.accountId()).orElseThrow(), bundle);
    }

    private byte[] ownerKey(byte value) {
        byte[] result = new byte[32];
        Arrays.fill(result, value);
        return result;
    }

    private record Enrolled(Account account, RegistrationCredentialFixture.CredentialBundle bundle) {
    }
}
