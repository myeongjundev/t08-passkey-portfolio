package dev.myeongjun.passkey.ceremony;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

public record CeremonyOptions(
        UUID id,
        CeremonyKind kind,
        byte[] challenge,
        byte[] pendingUserHandle,
        OffsetDateTime expiresAt
) {
    public CeremonyOptions {
        challenge = Arrays.copyOf(challenge, challenge.length);
        pendingUserHandle = pendingUserHandle == null
                ? null
                : Arrays.copyOf(pendingUserHandle, pendingUserHandle.length);
    }

    @Override
    public byte[] challenge() {
        return Arrays.copyOf(challenge, challenge.length);
    }

    @Override
    public byte[] pendingUserHandle() {
        return pendingUserHandle == null
                ? null
                : Arrays.copyOf(pendingUserHandle, pendingUserHandle.length);
    }
}
