package dev.myeongjun.passkey.config;

import dev.myeongjun.passkey.session.SessionAuthentication;
import dev.myeongjun.passkey.session.SessionPrincipal;
import dev.myeongjun.passkey.session.SessionSecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "spring.datasource.url=jdbc:h2:mem:t08-prod;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "t08.webauthn.rp-id=portfolio.example",
        "t08.webauthn.origin=https://portfolio.example"
})
class PersistentSessionConfigurationTests {

    @Autowired
    @SuppressWarnings("rawtypes")
    private SessionRepository sessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void productionSessionSurvivesSerializationInTheDatabase() {
        UUID accountId = UUID.randomUUID();
        Session session = sessionRepository.createSession();
        session.setAttribute(
                SessionAuthentication.PRINCIPAL_ATTRIBUTE,
                new SessionPrincipal(accountId, "합성 운영 계정")
        );
        session.setAttribute(SessionSecurityContext.CSRF_ATTRIBUTE, "synthetic-csrf");
        session.setAttribute(SessionSecurityContext.OWNER_KEY_ATTRIBUTE, new byte[32]);

        sessionRepository.save(session);

        Session restored = sessionRepository.findById(session.getId());
        assertThat(restored).isNotNull();
        assertThat((SessionPrincipal) restored.getAttribute(
                SessionAuthentication.PRINCIPAL_ATTRIBUTE
        )).isEqualTo(new SessionPrincipal(accountId, "합성 운영 계정"));
        assertThat(restored.<String>getAttribute(SessionSecurityContext.CSRF_ATTRIBUTE))
                .isEqualTo("synthetic-csrf");
        assertThat(restored.<byte[]>getAttribute(SessionSecurityContext.OWNER_KEY_ATTRIBUTE))
                .hasSize(32);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM spring_session WHERE session_id = ?",
                Integer.class,
                session.getId()
        )).isOne();
    }
}
