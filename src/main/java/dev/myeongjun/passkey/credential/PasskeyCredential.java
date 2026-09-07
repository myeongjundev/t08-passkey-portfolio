package dev.myeongjun.passkey.credential;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import dev.myeongjun.passkey.webauthn.VerifiedRegistration;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "passkey_credentials")
public class PasskeyCredential {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "credential_id", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] credentialId;

    @Column(name = "public_key_cose", nullable = false, columnDefinition = "BYTEA")
    private byte[] publicKeyCose;

    @Column(name = "credential_record", nullable = false, columnDefinition = "TEXT")
    private String credentialRecord;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    @Column(name = "transports", length = 255)
    private String transports;

    @Column(name = "backup_eligible", nullable = false)
    private boolean backupEligible;

    @Column(name = "backup_state", nullable = false)
    private boolean backupState;

    @Column(name = "nickname", nullable = false, length = 40)
    private String nickname;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    protected PasskeyCredential() {
    }

    public static PasskeyCredential create(
            UUID accountId,
            VerifiedRegistration verified,
            String nickname,
            OffsetDateTime registeredAt
    ) {
        PasskeyCredential credential = new PasskeyCredential();
        credential.id = UUID.randomUUID();
        credential.accountId = accountId;
        credential.credentialId = verified.credentialId();
        credential.publicKeyCose = verified.publicKeyCose();
        credential.credentialRecord = verified.credentialRecord();
        credential.signCount = verified.signCount();
        credential.transports = verified.transports();
        credential.backupEligible = verified.backupEligible();
        credential.backupState = verified.backupState();
        credential.nickname = nickname;
        credential.registeredAt = registeredAt;
        return credential;
    }

    public byte[] publicKeyCose() {
        return Arrays.copyOf(publicKeyCose, publicKeyCose.length);
    }

    public String credentialRecord() {
        return credentialRecord;
    }

    public UUID accountId() {
        return accountId;
    }

    public byte[] credentialId() {
        return Arrays.copyOf(credentialId, credentialId.length);
    }

    public long signCount() {
        return signCount;
    }

    public String transports() {
        return transports;
    }

    public void recordAuthentication(long newSignCount, boolean newBackupState, OffsetDateTime usedAt) {
        signCount = newSignCount;
        backupState = newBackupState;
        lastUsedAt = usedAt;
    }

    public UUID id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    public OffsetDateTime registeredAt() {
        return registeredAt;
    }
}
