package dev.myeongjun.passkey.webauthn;

import java.util.Arrays;

public record VerifiedRegistration(
        byte[] credentialId,
        byte[] publicKeyCose,
        String credentialRecord,
        long signCount,
        String transports,
        boolean backupEligible,
        boolean backupState
) {
    public VerifiedRegistration {
        credentialId = Arrays.copyOf(credentialId, credentialId.length);
        publicKeyCose = Arrays.copyOf(publicKeyCose, publicKeyCose.length);
    }

    @Override
    public byte[] credentialId() {
        return Arrays.copyOf(credentialId, credentialId.length);
    }

    @Override
    public byte[] publicKeyCose() {
        return Arrays.copyOf(publicKeyCose, publicKeyCose.length);
    }
}
