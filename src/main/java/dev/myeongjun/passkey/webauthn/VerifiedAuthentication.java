package dev.myeongjun.passkey.webauthn;

import java.util.Arrays;

public record VerifiedAuthentication(byte[] userHandle, long signCount, boolean backupState) {
    public VerifiedAuthentication {
        userHandle = userHandle == null ? null : Arrays.copyOf(userHandle, userHandle.length);
    }
    @Override public byte[] userHandle() {
        return userHandle == null ? null : Arrays.copyOf(userHandle, userHandle.length);
    }
}
