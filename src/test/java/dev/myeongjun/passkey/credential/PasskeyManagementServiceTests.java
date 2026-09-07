package dev.myeongjun.passkey.credential;

import com.webauthn4j.converter.util.ObjectConverter;
import dev.myeongjun.passkey.account.Account;
import dev.myeongjun.passkey.account.AccountAuthenticationService;
import dev.myeongjun.passkey.account.AccountRegistrationService;
import dev.myeongjun.passkey.account.AccountRepository;
import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyRepository;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.config.WebAuthnProperties;
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
class PasskeyManagementServiceTests {

    @Autowired PasskeyManagementService passkeyService;
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
    void addListDeleteAndAuthenticateWithSurvivorWhileDeletedKeyFails() {
        byte[] owner = ownerKey((byte) 41);
        CeremonyOptions firstRegistration = ceremonyService.issueCreateAccount(owner, "합성 복구 계정", "주 기기");
        var firstBundle = RegistrationCredentialFixture.createBundle(
                objectConverter, firstRegistration.challenge(), properties.rpId(), properties.origin().toString());
        var registered = registrationService.finish(firstRegistration.id(), owner, firstBundle.registrationJson());
        Account account = accountRepository.findById(registered.accountId()).orElseThrow();

        AddPasskeyOptions secondOptions = passkeyService.issueOptions(account.id(), owner, "예비 보안 키");
        assertThat(secondOptions.excludedCredentialIds()).singleElement()
                .satisfies(id -> assertThat(id).isEqualTo(firstBundle.credentialId()));
        var secondBundle = RegistrationCredentialFixture.createBundle(
                objectConverter, secondOptions.ceremony().challenge(), properties.rpId(), properties.origin().toString());
        passkeyService.finish(account.id(), secondOptions.ceremony().id(), owner, secondBundle.registrationJson());

        assertThat(passkeyService.list(account.id()))
                .extracting(PasskeyView::nickname)
                .containsExactly("주 기기", "예비 보안 키");
        PasskeyView first = passkeyService.list(account.id()).getFirst();
        passkeyService.delete(account.id(), first.id());
        assertThat(passkeyService.list(account.id())).singleElement()
                .extracting(PasskeyView::nickname).isEqualTo("예비 보안 키");

        authenticate(account, secondBundle, (byte) 42);

        byte[] deletedAttemptOwner = ownerKey((byte) 43);
        CeremonyOptions deletedAttempt = ceremonyService.issueAuthentication(deletedAttemptOwner);
        String deletedAssertion = firstBundle.authenticationJson(
                objectConverter, deletedAttempt.challenge(), properties.rpId(), properties.origin().toString(),
                account.userHandle(), 1, true);
        assertThatThrownBy(() -> authenticationService.finish(
                deletedAttempt.id(), deletedAttemptOwner, deletedAssertion))
                .isInstanceOf(AuthenticationVerificationException.class);

        PasskeyView survivor = passkeyService.list(account.id()).getFirst();
        assertThatThrownBy(() -> passkeyService.delete(account.id(), survivor.id()))
                .isInstanceOf(FinalPasskeyDeletionException.class);
        assertThat(passkeyService.list(account.id())).hasSize(1);
    }

    @Test
    void duplicateNicknameIsRejectedBeforeTheBrowserCeremonyStarts() {
        byte[] owner = ownerKey((byte) 51);
        CeremonyOptions registration = ceremonyService.issueCreateAccount(owner, "합성 중복 계정", "내 기기");
        var bundle = RegistrationCredentialFixture.createBundle(
                objectConverter, registration.challenge(), properties.rpId(), properties.origin().toString());
        var registered = registrationService.finish(registration.id(), owner, bundle.registrationJson());
        Account account = accountRepository.findById(registered.accountId()).orElseThrow();

        long ceremoniesBefore = ceremonyRepository.count();

        // The nickname collides with the credential created above. This must fail here,
        // before navigator.credentials.create() puts a real passkey on the device.
        assertThatThrownBy(() -> passkeyService.issueOptions(account.id(), owner, "내 기기"))
                .isInstanceOf(DuplicatePasskeyNicknameException.class);
        assertThatThrownBy(() -> passkeyService.issueOptions(account.id(), owner, "  내 기기  "))
                .isInstanceOf(DuplicatePasskeyNicknameException.class);

        // No ceremony was created by the rejected attempts, and the account still holds
        // exactly its first passkey.
        assertThat(ceremonyRepository.count()).isEqualTo(ceremoniesBefore);
        assertThat(passkeyService.list(account.id())).singleElement()
                .extracting(PasskeyView::nickname).isEqualTo("내 기기");

        // A distinct nickname still works.
        AddPasskeyOptions options = passkeyService.issueOptions(account.id(), owner, "예비 기기");
        var second = RegistrationCredentialFixture.createBundle(
                objectConverter, options.ceremony().challenge(), properties.rpId(), properties.origin().toString());
        passkeyService.finish(account.id(), options.ceremony().id(), owner, second.registrationJson());
        assertThat(passkeyService.list(account.id())).extracting(PasskeyView::nickname)
                .containsExactly("내 기기", "예비 기기");
    }

    private void authenticate(Account account,
                              RegistrationCredentialFixture.CredentialBundle bundle,
                              byte ownerValue) {
        byte[] loginOwner = ownerKey(ownerValue);
        CeremonyOptions login = ceremonyService.issueAuthentication(loginOwner);
        String assertion = bundle.authenticationJson(
                objectConverter, login.challenge(), properties.rpId(), properties.origin().toString(),
                account.userHandle(), 1, true);
        assertThat(authenticationService.finish(login.id(), loginOwner, assertion).accountId())
                .isEqualTo(account.id());
    }

    private byte[] ownerKey(byte value) {
        byte[] result = new byte[32];
        Arrays.fill(result, value);
        return result;
    }
}
