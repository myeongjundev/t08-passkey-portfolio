package dev.myeongjun.passkey.credential;

import dev.myeongjun.passkey.account.Account;
import dev.myeongjun.passkey.account.AccountRepository;
import dev.myeongjun.passkey.ceremony.CeremonyKind;
import dev.myeongjun.passkey.ceremony.CeremonyLease;
import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.webauthn.VerifiedRegistration;
import dev.myeongjun.passkey.webauthn.WebAuthnVerificationAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class PasskeyManagementService {

    private final CeremonyService ceremonyService;
    private final WebAuthnVerificationAdapter adapter;
    private final AccountRepository accountRepository;
    private final PasskeyCredentialRepository credentialRepository;
    private final Clock clock;

    public PasskeyManagementService(CeremonyService ceremonyService,
                                    WebAuthnVerificationAdapter adapter,
                                    AccountRepository accountRepository,
                                    PasskeyCredentialRepository credentialRepository,
                                    Clock clock) {
        this.ceremonyService = ceremonyService;
        this.adapter = adapter;
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
        this.clock = clock;
    }

    @Transactional
    public AddPasskeyOptions issueOptions(UUID accountId, byte[] ownerKey, String nickname) {
        Account account = accountRepository.findById(accountId).orElseThrow(PasskeyNotFoundException::new);
        CeremonyOptions ceremony = ceremonyService.issueAddPasskey(ownerKey, accountId, nickname);
        List<byte[]> existing = credentialRepository.findAllByAccountIdOrderByRegisteredAt(accountId).stream()
                .map(PasskeyCredential::credentialId)
                .toList();
        return new AddPasskeyOptions(ceremony, account.userHandle(), account.displayName(), existing);
    }

    @Transactional
    public PasskeyView finish(UUID accountId, UUID ceremonyId, byte[] ownerKey, String credentialJson) {
        CeremonyLease lease = ceremonyService.consume(ceremonyId, CeremonyKind.ADD_PASSKEY, ownerKey);
        if (!accountId.equals(lease.accountId())) {
            throw new PasskeyNotFoundException();
        }
        VerifiedRegistration verified = adapter.verifyRegistration(credentialJson, lease.challenge());
        PasskeyCredential saved = credentialRepository.save(PasskeyCredential.create(
                accountId, verified, lease.pendingNickname(), now()));
        return view(saved);
    }

    @Transactional(readOnly = true)
    public List<PasskeyView> list(UUID accountId) {
        return credentialRepository.findAllByAccountIdOrderByRegisteredAt(accountId).stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public void delete(UUID accountId, UUID passkeyId) {
        List<PasskeyCredential> credentials = credentialRepository.findAllByAccountIdForUpdate(accountId);
        PasskeyCredential target = credentials.stream()
                .filter(credential -> credential.id().equals(passkeyId))
                .findFirst()
                .orElseThrow(PasskeyNotFoundException::new);
        if (credentials.size() <= 1) {
            throw new FinalPasskeyDeletionException();
        }
        credentialRepository.delete(target);
    }

    private PasskeyView view(PasskeyCredential credential) {
        return new PasskeyView(credential.id(), credential.nickname(), credential.registeredAt());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
