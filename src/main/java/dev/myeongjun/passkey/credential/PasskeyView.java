package dev.myeongjun.passkey.credential;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PasskeyView(UUID id, String nickname, OffsetDateTime registeredAt) {
}
