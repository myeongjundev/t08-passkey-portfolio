package dev.myeongjun.passkey.credential;

import dev.myeongjun.passkey.ceremony.CeremonyOptions;

import java.util.Arrays;
import java.util.List;

public record AddPasskeyOptions(
        CeremonyOptions ceremony,
        byte[] userHandle,
        String displayName,
        List<byte[]> excludedCredentialIds
) {
    public AddPasskeyOptions {
        userHandle = Arrays.copyOf(userHandle, userHandle.length);
        excludedCredentialIds = excludedCredentialIds.stream()
                .map(value -> Arrays.copyOf(value, value.length))
                .toList();
    }
    @Override public byte[] userHandle() { return Arrays.copyOf(userHandle, userHandle.length); }
}
