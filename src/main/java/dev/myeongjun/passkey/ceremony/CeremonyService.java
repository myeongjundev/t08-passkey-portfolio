package dev.myeongjun.passkey.ceremony;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CeremonyService {

    public static final int RANDOM_VALUE_BYTES = 32;
    public static final Duration TIME_TO_LIVE = Duration.ofMinutes(5);

    private final CeremonyRepository ceremonyRepository;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public CeremonyService(CeremonyRepository ceremonyRepository, Clock clock) {
        this.ceremonyRepository = ceremonyRepository;
        this.clock = clock;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public CeremonyOptions issueCreateAccount(byte[] ownerKey, String displayName, String nickname) {
        requireOpaqueOwnerKey(ownerKey);
        requireLabel(displayName, "displayName");
        requireLabel(nickname, "nickname");
        return issue(
                CeremonyKind.CREATE_ACCOUNT,
                ownerKey,
                null,
                randomBytes(),
                displayName.strip(),
                nickname.strip()
        );
    }

    @Transactional
    public CeremonyOptions issueAuthentication(byte[] ownerKey) {
        requireOpaqueOwnerKey(ownerKey);
        return issue(CeremonyKind.AUTHENTICATE, ownerKey, null, null, null, null);
    }

    @Transactional
    public CeremonyOptions issueAddPasskey(byte[] ownerKey, UUID accountId, String nickname) {
        requireOpaqueOwnerKey(ownerKey);
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        requireLabel(nickname, "nickname");
        return issue(CeremonyKind.ADD_PASSKEY, ownerKey, accountId, null, null, nickname.strip());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = CeremonyRejectedException.class)
    public CeremonyLease consume(UUID ceremonyId, CeremonyKind expectedKind, byte[] ownerKey) {
        requireOpaqueOwnerKey(ownerKey);
        WebAuthnCeremony ceremony = ceremonyRepository.findByIdForUpdate(ceremonyId)
                .orElseThrow(() -> new CeremonyRejectedException(CeremonyRejection.NOT_FOUND));

        if (!ceremony.ownedBy(ownerKey)) {
            throw new CeremonyRejectedException(CeremonyRejection.OWNER_MISMATCH);
        }
        if (ceremony.consumedAt() != null) {
            throw new CeremonyRejectedException(CeremonyRejection.REPLAYED);
        }

        OffsetDateTime now = now();
        ceremony.consume(now);
        ceremonyRepository.saveAndFlush(ceremony);

        if (ceremony.kind() != expectedKind) {
            throw new CeremonyRejectedException(CeremonyRejection.KIND_MISMATCH);
        }
        if (!ceremony.expiresAt().isAfter(now)) {
            throw new CeremonyRejectedException(CeremonyRejection.EXPIRED);
        }
        return ceremony.toLease();
    }

    private CeremonyOptions issue(
            CeremonyKind kind,
            byte[] ownerKey,
            UUID accountId,
            byte[] pendingUserHandle,
            String pendingDisplayName,
            String pendingNickname
    ) {
        OffsetDateTime now = now();
        ceremonyRepository.findActiveForOwner(ownerKey, kind).ifPresent(active -> {
            active.consume(now);
            ceremonyRepository.saveAndFlush(active);
        });

        WebAuthnCeremony ceremony = WebAuthnCeremony.create(
                kind,
                randomBytes(),
                ownerKey,
                accountId,
                pendingUserHandle,
                pendingDisplayName,
                pendingNickname,
                now,
                now.plus(TIME_TO_LIVE)
        );
        return ceremonyRepository.saveAndFlush(ceremony).toOptions();
    }

    private byte[] randomBytes() {
        byte[] value = new byte[RANDOM_VALUE_BYTES];
        secureRandom.nextBytes(value);
        return value;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private void requireOpaqueOwnerKey(byte[] ownerKey) {
        if (ownerKey == null || ownerKey.length != RANDOM_VALUE_BYTES) {
            throw new IllegalArgumentException("ownerKey must be a 32-byte opaque value");
        }
    }

    private void requireLabel(String value, String field) {
        if (value == null || value.isBlank() || value.strip().length() > 40) {
            throw new IllegalArgumentException(field + " must contain 1 to 40 characters");
        }
    }
}
