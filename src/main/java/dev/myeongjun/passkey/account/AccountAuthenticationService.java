package dev.myeongjun.passkey.account;

import dev.myeongjun.passkey.ceremony.CeremonyKind;
import dev.myeongjun.passkey.ceremony.CeremonyLease;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.credential.PasskeyCredential;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.webauthn.AuthenticationVerificationException;
import dev.myeongjun.passkey.webauthn.StoredAuthenticator;
import dev.myeongjun.passkey.webauthn.VerifiedAuthentication;
import dev.myeongjun.passkey.webauthn.WebAuthnVerificationAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;

@Service
public class AccountAuthenticationService {

    private final CeremonyService ceremonyService;
    private final WebAuthnVerificationAdapter adapter;
    private final PasskeyCredentialRepository credentialRepository;
    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountAuthenticationService(CeremonyService ceremonyService,
                                        WebAuthnVerificationAdapter adapter,
                                        PasskeyCredentialRepository credentialRepository,
                                        AccountRepository accountRepository,
                                        Clock clock) {
        this.ceremonyService = ceremonyService;
        this.adapter = adapter;
        this.credentialRepository = credentialRepository;
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Transactional
    public RegisteredAccount finish(UUID ceremonyId, byte[] ownerKey, String credentialJson) {
        CeremonyLease lease = ceremonyService.consume(ceremonyId, CeremonyKind.AUTHENTICATE, ownerKey);
        byte[] credentialId = adapter.authenticationCredentialId(credentialJson);
        PasskeyCredential credential = credentialRepository.findByCredentialIdForUpdate(credentialId)
                .orElseThrow(() -> new AuthenticationVerificationException(
                        new IllegalArgumentException("unknown credential")));
        Account account = accountRepository.findById(credential.accountId())
                .orElseThrow(() -> new AuthenticationVerificationException(
                        new IllegalArgumentException("unknown account")));
        VerifiedAuthentication verified = adapter.verifyAuthentication(
                credentialJson,
                lease.challenge(),
                new StoredAuthenticator(
                        credential.credentialId(), credential.publicKeyCose(),
                        credential.signCount(), credential.transports())
        );
        if (verified.userHandle() == null || !Arrays.equals(verified.userHandle(), account.userHandle())) {
            throw new AuthenticationVerificationException(new IllegalArgumentException("user handle mismatch"));
        }
        if ((credential.signCount() != 0 || verified.signCount() != 0)
                && verified.signCount() <= credential.signCount()) {
            throw new AuthenticationVerificationException(new IllegalArgumentException("sign counter did not advance"));
        }
        credential.recordAuthentication(
                verified.signCount(), verified.backupState(),
                OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return new RegisteredAccount(account.id(), account.displayName());
    }
}
