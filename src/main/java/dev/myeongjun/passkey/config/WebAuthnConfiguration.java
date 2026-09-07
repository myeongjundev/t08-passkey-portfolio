package dev.myeongjun.passkey.config;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebAuthnConfiguration {

    @Bean
    ObjectConverter webAuthnObjectConverter() {
        return new ObjectConverter();
    }

    @Bean
    WebAuthnManager webAuthnManager(ObjectConverter objectConverter) {
        return WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }
}
