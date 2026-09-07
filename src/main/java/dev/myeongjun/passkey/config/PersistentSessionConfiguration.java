package dev.myeongjun.passkey.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

/** Keeps authenticated sessions valid across stateless production containers. */
@Configuration
@Profile("prod")
@EnableJdbcHttpSession
public class PersistentSessionConfiguration {
}
