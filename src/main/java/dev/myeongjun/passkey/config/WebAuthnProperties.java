package dev.myeongjun.passkey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

@ConfigurationProperties(prefix = "t08.webauthn")
public record WebAuthnProperties(
        String rpId,
        String rpName,
        URI origin,
        Duration timeout
) {
    public WebAuthnProperties {
        if (rpId == null || rpId.isBlank()) {
            throw new IllegalArgumentException("t08.webauthn.rp-id is required");
        }
        if (rpName == null || rpName.isBlank()) {
            throw new IllegalArgumentException("t08.webauthn.rp-name is required");
        }
        if (origin == null || origin.getScheme() == null || origin.getHost() == null) {
            throw new IllegalArgumentException("t08.webauthn.origin must be an absolute origin");
        }
        if (origin.getRawUserInfo() != null
                || origin.getRawQuery() != null
                || origin.getRawFragment() != null
                || (origin.getRawPath() != null && !origin.getRawPath().isEmpty())) {
            throw new IllegalArgumentException("t08.webauthn.origin must not contain credentials, path, query or fragment");
        }
        boolean localhostHttp = "http".equalsIgnoreCase(origin.getScheme())
                && "localhost".equalsIgnoreCase(origin.getHost());
        if (!"https".equalsIgnoreCase(origin.getScheme()) && !localhostHttp) {
            throw new IllegalArgumentException("t08.webauthn.origin must use HTTPS except on localhost");
        }
        String originHost = origin.getHost().toLowerCase(Locale.ROOT);
        String relyingPartyId = rpId.toLowerCase(Locale.ROOT);
        if (!originHost.equals(relyingPartyId) && !originHost.endsWith("." + relyingPartyId)) {
            throw new IllegalArgumentException("t08.webauthn.rp-id must match the origin host or its registrable suffix");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("t08.webauthn.timeout must be positive");
        }
    }
}
