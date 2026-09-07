package dev.myeongjun.passkey.ceremony;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CeremonyServiceTests {

    private static final byte[] OWNER_A = repeated((byte) 0x11);
    private static final byte[] OWNER_B = repeated((byte) 0x22);

    @Autowired
    private CeremonyService ceremonyService;

    @Autowired
    private CeremonyRepository ceremonyRepository;

    @BeforeEach
    void clearCeremonies() {
        ceremonyRepository.deleteAll();
    }

    @Test
    void createsFreshServerHeldValuesAndKeepsOnlyOneActiveSlot() {
        CeremonyOptions first = ceremonyService.issueCreateAccount(OWNER_A, "합성 계정 A", "노트북 패스키");
        CeremonyOptions second = ceremonyService.issueCreateAccount(OWNER_A, "합성 계정 A", "노트북 패스키");

        assertThat(first.challenge()).hasSize(CeremonyService.RANDOM_VALUE_BYTES);
        assertThat(first.pendingUserHandle()).hasSize(CeremonyService.RANDOM_VALUE_BYTES);
        assertThat(second.challenge()).isNotEqualTo(first.challenge());
        assertThat(second.pendingUserHandle()).isNotEqualTo(first.pendingUserHandle());
        assertThat(first.expiresAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(4));
        assertThat(ceremonyRepository.countByOwnerKeyAndKindAndActiveSlot(
                OWNER_A,
                CeremonyKind.CREATE_ACCOUNT,
                WebAuthnCeremony.ACTIVE_SLOT
        )).isEqualTo(1);

        assertRejected(first, CeremonyKind.CREATE_ACCOUNT, OWNER_A, CeremonyRejection.REPLAYED);
        assertThat(ceremonyService.consume(second.id(), CeremonyKind.CREATE_ACCOUNT, OWNER_A).challenge())
                .isEqualTo(second.challenge());
    }

    @Test
    void consumesOnceAndRejectsReplay() {
        CeremonyOptions options = ceremonyService.issueAuthentication(OWNER_A);

        CeremonyLease lease = ceremonyService.consume(options.id(), CeremonyKind.AUTHENTICATE, OWNER_A);

        assertThat(lease.challenge()).isEqualTo(options.challenge());
        assertRejected(options, CeremonyKind.AUTHENTICATE, OWNER_A, CeremonyRejection.REPLAYED);
    }

    @Test
    void wrongOwnerCannotBurnTheCeremony() {
        CeremonyOptions options = ceremonyService.issueAuthentication(OWNER_A);

        assertRejected(options, CeremonyKind.AUTHENTICATE, OWNER_B, CeremonyRejection.OWNER_MISMATCH);
        assertThat(ceremonyService.consume(options.id(), CeremonyKind.AUTHENTICATE, OWNER_A).id())
                .isEqualTo(options.id());
    }

    @Test
    void kindMismatchConsumesTheFirstFinishAttempt() {
        CeremonyOptions options = ceremonyService.issueAuthentication(OWNER_A);

        assertRejected(options, CeremonyKind.CREATE_ACCOUNT, OWNER_A, CeremonyRejection.KIND_MISMATCH);
        assertRejected(options, CeremonyKind.AUTHENTICATE, OWNER_A, CeremonyRejection.REPLAYED);
    }

    @Test
    void expiredCeremonyIsRejectedAndCannotBeRetried() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        WebAuthnCeremony expired = WebAuthnCeremony.create(
                CeremonyKind.AUTHENTICATE,
                repeated((byte) 0x33),
                OWNER_A,
                null,
                null,
                null,
                null,
                now.minusMinutes(10),
                now.minusMinutes(5)
        );
        CeremonyOptions options = ceremonyRepository.saveAndFlush(expired).toOptions();

        assertRejected(options, CeremonyKind.AUTHENTICATE, OWNER_A, CeremonyRejection.EXPIRED);
        assertRejected(options, CeremonyKind.AUTHENTICATE, OWNER_A, CeremonyRejection.REPLAYED);
    }

    @Test
    void pessimisticLockAllowsOnlyOneConcurrentConsumer() throws Exception {
        CeremonyOptions options = ceremonyService.issueAuthentication(OWNER_A);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> attempt = () -> {
                try {
                    ceremonyService.consume(options.id(), CeremonyKind.AUTHENTICATE, OWNER_A);
                    return "SUCCESS";
                } catch (CeremonyRejectedException exception) {
                    return exception.reason().name();
                }
            };

            List<Future<String>> futures = executor.invokeAll(List.of(attempt, attempt));
            List<String> outcomes = List.of(futures.get(0).get(), futures.get(1).get());

            assertThat(outcomes).containsExactlyInAnyOrder("SUCCESS", CeremonyRejection.REPLAYED.name());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void challengeFingerprintIsShortAndDoesNotExposeTheRawValue() {
        CeremonyOptions options = ceremonyService.issueAuthentication(OWNER_A);
        String fingerprint = ChallengeFingerprint.of(options.challenge());

        assertThat(fingerprint).hasSize(12).matches("[0-9a-f]{12}");
        assertThat(fingerprint).isNotEqualTo(Base64.getUrlEncoder().withoutPadding().encodeToString(options.challenge()));
    }

    @Test
    void rejectsNonOpaqueOwnerKeysAndInvalidLabelsBeforePersistence() {
        assertThatThrownBy(() -> ceremonyService.issueAuthentication(new byte[8]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ceremonyService.issueCreateAccount(OWNER_A, " ", "패스키"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ceremonyRepository.count()).isZero();
    }

    private void assertRejected(
            CeremonyOptions options,
            CeremonyKind expectedKind,
            byte[] ownerKey,
            CeremonyRejection rejection
    ) {
        assertThatThrownBy(() -> ceremonyService.consume(options.id(), expectedKind, ownerKey))
                .isInstanceOfSatisfying(
                        CeremonyRejectedException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(rejection)
                );
    }

    private static byte[] repeated(byte value) {
        byte[] bytes = new byte[CeremonyService.RANDOM_VALUE_BYTES];
        Arrays.fill(bytes, value);
        return bytes;
    }
}
