package dev.myeongjun.passkey.ceremony;

import java.util.Arrays;
import java.util.UUID;

public record CeremonyLease(
        UUID id,
        CeremonyKind kind,
        byte[] challenge,
        UUID accountId,
        byte[] pendingUserHandle,
        String pendingDisplayName,
        String pendingNickname
) {
    public CeremonyLease {
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
