package dev.myeongjun.passkey.account;

import dev.myeongjun.passkey.ceremony.CeremonyKind;
import dev.myeongjun.passkey.ceremony.CeremonyLease;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.credential.PasskeyCredential;
import dev.myeongjun.passkey.credential.PasskeyCredentialRepository;
import dev.myeongjun.passkey.privatearea.PrivateItem;
import dev.myeongjun.passkey.privatearea.PrivateItemCategory;
import dev.myeongjun.passkey.privatearea.PrivateItemRepository;
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
public class AccountRegistrationService {

    private final CeremonyService ceremonyService;
    private final WebAuthnVerificationAdapter verificationAdapter;
    private final AccountRepository accountRepository;
    private final PasskeyCredentialRepository credentialRepository;
    private final PrivateItemRepository privateItemRepository;
    private final Clock clock;

    public AccountRegistrationService(
            CeremonyService ceremonyService,
            WebAuthnVerificationAdapter verificationAdapter,
            AccountRepository accountRepository,
            PasskeyCredentialRepository credentialRepository,
            PrivateItemRepository privateItemRepository,
            Clock clock
    ) {
        this.ceremonyService = ceremonyService;
        this.verificationAdapter = verificationAdapter;
        this.accountRepository = accountRepository;
        this.credentialRepository = credentialRepository;
        this.privateItemRepository = privateItemRepository;
        this.clock = clock;
    }

    @Transactional
    public RegisteredAccount finish(UUID ceremonyId, byte[] ownerKey, String credentialJson) {
        CeremonyLease lease = ceremonyService.consume(ceremonyId, CeremonyKind.CREATE_ACCOUNT, ownerKey);
        VerifiedRegistration verified = verificationAdapter.verifyRegistration(credentialJson, lease.challenge());
        OffsetDateTime now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        Account account = accountRepository.save(Account.create(
                lease.pendingUserHandle(),
                lease.pendingDisplayName(),
                now
        ));
        credentialRepository.save(PasskeyCredential.create(
                account.id(),
                verified,
                lease.pendingNickname(),
                now
        ));
        privateItemRepository.saveAll(syntheticItems(account.id(), account.displayName(), now));
        return new RegisteredAccount(account.id(), account.displayName());
    }

    private List<PrivateItem> syntheticItems(UUID accountId, String displayName, OffsetDateTime now) {
        return List.of(
                PrivateItem.create(accountId, PrivateItemCategory.PROJECT,
                        "합성 프로젝트 알파 출시 메모",
                        displayName + " 전용 샘플 API 계약 검토와 화면 점검 순서를 기록했습니다.", 0, now),
                PrivateItem.create(accountId, PrivateItemCategory.TARGET,
                        "합성 지원 기업 베타 목록",
                        displayName + " 전용 가상 기업의 기술 스택과 지원 준비 항목을 정리했습니다.", 1, now),
                PrivateItem.create(accountId, PrivateItemCategory.RETROSPECTIVE,
                        "합성 주간 회고 감마",
                        displayName + " 전용 샘플 일정에서 배운 점과 다음 실험을 적었습니다.", 2, now)
        );
    }
}
