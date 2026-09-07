package dev.myeongjun.passkey.evidence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private SecurityEventOutcome outcome;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "ceremony_id")
    private UUID ceremonyId;

    @Column(name = "credential_fingerprint", length = 64)
    private String credentialFingerprint;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected SecurityEvent() {
    }
}
