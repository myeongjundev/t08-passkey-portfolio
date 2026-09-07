package dev.myeongjun.passkey.webauthn;

import java.util.Arrays;

public record StoredAuthenticator(
        byte[] credentialId,
        byte[] publicKeyCose,
        long signCount,
        String transports
) {
    public StoredAuthenticator {
        credentialId = Arrays.copyOf(credentialId, credentialId.length);
        publicKeyCose = Arrays.copyOf(publicKeyCose, publicKeyCose.length);
    }

    @Override public byte[] credentialId() { return Arrays.copyOf(credentialId, credentialId.length); }
    @Override public byte[] publicKeyCose() { return Arrays.copyOf(publicKeyCose, publicKeyCose.length); }
}
