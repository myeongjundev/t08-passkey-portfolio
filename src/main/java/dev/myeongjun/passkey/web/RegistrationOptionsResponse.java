package dev.myeongjun.passkey.web;

import java.util.List;
import java.util.UUID;

public record RegistrationOptionsResponse(
        UUID ceremonyId,
        PublicKeyCreationOptions publicKey
) {
    public record PublicKeyCreationOptions(
            String challenge,
            RelyingParty rp,
            User user,
            List<CredentialParameter> pubKeyCredParams,
            long timeout,
            AuthenticatorSelection authenticatorSelection,
            String attestation,
            List<Object> excludeCredentials
    ) {
    }

    public record RelyingParty(String id, String name) {
    }

    public record User(String id, String name, String displayName) {
    }

    public record CredentialParameter(String type, int alg) {
    }

    public record AuthenticatorSelection(
            String residentKey,
            boolean requireResidentKey,
            String userVerification
    ) {
    }
}
