package dev.myeongjun.passkey.privatearea;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "private_items")
public class PrivateItem {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PrivateItemCategory category;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "body", nullable = false, length = 1000)
    private String body;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected PrivateItem() {
    }

    public static PrivateItem create(
            UUID accountId,
            PrivateItemCategory category,
            String title,
            String body,
            int sortOrder,
            OffsetDateTime createdAt
    ) {
        PrivateItem item = new PrivateItem();
        item.id = UUID.randomUUID();
        item.accountId = accountId;
        item.category = category;
        item.title = title;
        item.body = body;
        item.sortOrder = sortOrder;
        item.createdAt = createdAt;
        return item;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    public UUID id() {
        return id;
    }

    public UUID accountId() {
        return accountId;
    }
}
