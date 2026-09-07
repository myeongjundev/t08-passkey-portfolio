package dev.myeongjun.passkey.session;

import jakarta.servlet.http.HttpSession;

import java.util.Optional;

public final class SessionAuthentication {

    public static final String PRINCIPAL_ATTRIBUTE = "t08.session.principal";

    private SessionAuthentication() {
    }

    public static Optional<SessionPrincipal> current(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(PRINCIPAL_ATTRIBUTE);
        return value instanceof SessionPrincipal principal
                ? Optional.of(principal)
                : Optional.empty();
    }
}
