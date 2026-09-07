package dev.myeongjun.passkey.account;

import com.webauthn4j.converter.util.ObjectConverter;
import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyRejectedException;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.privatearea.PrivateItemRepository;
import dev.myeongjun.passkey.webauthn.RegistrationCredentialFixture;
import dev.myeongjun.passkey.webauthn.RegistrationVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AccountRegistrationServiceTests {

    @Autowired AccountRegistrationService registrationService;
    @Autowired CeremonyService ceremonyService;
    @Autowired AccountRepository accountRepository;
    @Autowired PasskeyCredentialRepository credentialRepository;
    @Autowired PrivateItemRepository privateItemRepository;
    @Autowired ObjectConverter objectConverter;
    @Autowired WebAuthnProperties properties;

    @BeforeEach
    void cleanAccountData() {
        privateItemRepository.deleteAll();
        credentialRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void verifiedRegistrationPersistsOnlyPublicCredentialMaterialAndSyntheticItems() {
        byte[] ownerKey = ownerKey((byte) 7);
        CeremonyOptions options = ceremonyService.issueCreateAccount(ownerKey, "합성 사용자", "이 노트북");
        String credential = RegistrationCredentialFixture.create(
                objectConverter, options.challenge(), properties.rpId(), properties.origin().toString());

        RegisteredAccount account = registrationService.finish(options.id(), ownerKey, credential);

        assertThat(account.displayName()).isEqualTo("합성 사용자");
        assertThat(accountRepository.count()).isOne();
        assertThat(credentialRepository.count()).isOne();
        assertThat(privateItemRepository.findAllByAccountIdOrderBySortOrder(account.accountId())).hasSize(3);
        var stored = credentialRepository.findAll().getFirst();
        assertThat(stored.publicKeyCose()).isNotEmpty();
        assertThat(stored.credentialRecord()).contains("\"formatVersion\":1");
        assertThat(stored.credentialRecord()).doesNotContain("challenge", "clientDataJSON", "private");
    }

    @Test
    void invalidOriginCreatesNoAccountAndBurnsChallenge() {
        byte[] ownerKey = ownerKey((byte) 8);
        CeremonyOptions options = ceremonyService.issueCreateAccount(ownerKey, "합성 사용자", "이 노트북");
        String attackerCredential = RegistrationCredentialFixture.create(
                objectConverter, options.challenge(), properties.rpId(), "https://attacker.example");

        assertThatThrownBy(() -> registrationService.finish(options.id(), ownerKey, attackerCredential))
                .isInstanceOf(RegistrationVerificationException.class);
        assertThat(accountRepository.count()).isZero();
        assertThat(credentialRepository.count()).isZero();
        assertThat(privateItemRepository.count()).isZero();
        assertThatThrownBy(() -> registrationService.finish(options.id(), ownerKey, attackerCredential))
                .isInstanceOf(CeremonyRejectedException.class);
    }

    private byte[] ownerKey(byte value) {
        byte[] result = new byte[32];
        Arrays.fill(result, value);
        return result;
    }
}
