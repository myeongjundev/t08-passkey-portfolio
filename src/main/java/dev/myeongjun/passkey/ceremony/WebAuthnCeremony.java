package dev.myeongjun.passkey.ceremony;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "webauthn_ceremonies")
public class WebAuthnCeremony {

    static final String ACTIVE_SLOT = "ACTIVE";

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private CeremonyKind kind;

    @Column(name = "challenge", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] challenge;

    @Column(name = "owner_key", nullable = false, columnDefinition = "BYTEA")
    private byte[] ownerKey;

    @Column(name = "active_slot", length = 8)
    private String activeSlot;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "pending_user_handle", columnDefinition = "BYTEA")
    private byte[] pendingUserHandle;

    @Column(name = "pending_display_name", length = 40)
    private String pendingDisplayName;

    @Column(name = "pending_nickname", length = 40)
    private String pendingNickname;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected WebAuthnCeremony() {
    }

    static WebAuthnCeremony create(
            CeremonyKind kind,
            byte[] challenge,
            byte[] ownerKey,
            UUID accountId,
            byte[] pendingUserHandle,
            String pendingDisplayName,
            String pendingNickname,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt
    ) {
        WebAuthnCeremony ceremony = new WebAuthnCeremony();
        ceremony.id = UUID.randomUUID();
        ceremony.kind = kind;
        ceremony.challenge = Arrays.copyOf(challenge, challenge.length);
        ceremony.ownerKey = Arrays.copyOf(ownerKey, ownerKey.length);
        ceremony.activeSlot = ACTIVE_SLOT;
        ceremony.accountId = accountId;
        ceremony.pendingUserHandle = pendingUserHandle == null
                ? null
                : Arrays.copyOf(pendingUserHandle, pendingUserHandle.length);
        ceremony.pendingDisplayName = pendingDisplayName;
        ceremony.pendingNickname = pendingNickname;
        ceremony.createdAt = createdAt;
        ceremony.expiresAt = expiresAt;
        return ceremony;
    }

    void consume(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
        this.activeSlot = null;
    }

    CeremonyOptions toOptions() {
        return new CeremonyOptions(id, kind, challenge, pendingUserHandle, expiresAt);
    }

    CeremonyLease toLease() {
        return new CeremonyLease(
                id,
                kind,
                challenge,
                accountId,
                pendingUserHandle,
                pendingDisplayName,
                pendingNickname
        );
    }

    boolean ownedBy(byte[] candidateOwnerKey) {
        return MessageDigest.isEqual(ownerKey, candidateOwnerKey);
    }

    UUID id() {
        return id;
    }

    CeremonyKind kind() {
        return kind;
    }

    OffsetDateTime expiresAt() {
        return expiresAt;
    }

    OffsetDateTime consumedAt() {
        return consumedAt;
    }
}
