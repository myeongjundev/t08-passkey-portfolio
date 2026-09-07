package dev.myeongjun.passkey.web;

import java.util.List;
import java.util.UUID;

public record AuthenticationOptionsResponse(UUID ceremonyId, PublicKeyRequestOptions publicKey) {
    public record PublicKeyRequestOptions(
            String challenge,
            long timeout,
            String rpId,
            String userVerification,
            List<Object> allowCredentials
    ) {
    }
}
