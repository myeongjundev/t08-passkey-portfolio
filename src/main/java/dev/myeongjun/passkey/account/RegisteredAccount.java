package dev.myeongjun.passkey.account;

import java.util.UUID;

public record RegisteredAccount(UUID accountId, String displayName) {
}
