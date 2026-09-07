package dev.myeongjun.passkey.config;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebAuthnPropertiesTests {

    @Test
    void acceptsHttpsRpDomainAndLocalhostHttp() {
        assertThatCode(() -> properties("example.com", "https://login.example.com"))
                .doesNotThrowAnyException();
        assertThatCode(() -> properties("localhost", "http://localhost:8080"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPlainHttpOutsideLocalhost() {
        assertThatThrownBy(() -> properties("example.com", "http://example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOriginWithPathOrUnrelatedRpId() {
        assertThatThrownBy(() -> properties("example.com", "https://example.com/login"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties("other.example", "https://example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private WebAuthnProperties properties(String rpId, String origin) {
        return new WebAuthnProperties(rpId, "T08 test", URI.create(origin), Duration.ofMinutes(5));
    }
}
