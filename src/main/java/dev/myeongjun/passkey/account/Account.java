package dev.myeongjun.passkey.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "user_handle", nullable = false, unique = true, columnDefinition = "BYTEA")
    private byte[] userHandle;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Account() {
    }

    public static Account create(byte[] userHandle, String displayName, OffsetDateTime createdAt) {
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.userHandle = Arrays.copyOf(userHandle, userHandle.length);
        account.displayName = displayName;
        account.createdAt = createdAt;
        return account;
    }

    public UUID id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public byte[] userHandle() {
        return Arrays.copyOf(userHandle, userHandle.length);
    }
}
