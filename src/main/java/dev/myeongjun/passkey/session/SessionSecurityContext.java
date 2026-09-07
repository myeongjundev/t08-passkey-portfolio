package dev.myeongjun.passkey.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import dev.myeongjun.passkey.account.RegisteredAccount;

@Component
public class SessionSecurityContext {

    public static final String OWNER_KEY_ATTRIBUTE = "t08.session.ceremony-owner";
    public static final String CSRF_ATTRIBUTE = "t08.session.csrf";
    private static final int RANDOM_VALUE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public void prepare(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        if (!(session.getAttribute(OWNER_KEY_ATTRIBUTE) instanceof byte[])) {
            session.setAttribute(OWNER_KEY_ATTRIBUTE, randomBytes());
        }
        if (!(session.getAttribute(CSRF_ATTRIBUTE) instanceof String)) {
            session.setAttribute(
                    CSRF_ATTRIBUTE,
                    Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes())
            );
        }
    }

    public byte[] ownerKey(HttpServletRequest request) {
        prepare(request);
        byte[] value = (byte[]) request.getSession(false).getAttribute(OWNER_KEY_ATTRIBUTE);
        return Arrays.copyOf(value, value.length);
    }

    public String csrfToken(HttpServletRequest request) {
        prepare(request);
        return (String) request.getSession(false).getAttribute(CSRF_ATTRIBUTE);
    }

    public void authenticate(HttpServletRequest request, RegisteredAccount account) {
        HttpSession current = request.getSession(false);
        if (current != null) {
            current.invalidate();
        }
        HttpSession authenticated = request.getSession(true);
        authenticated.setAttribute(
                SessionAuthentication.PRINCIPAL_ATTRIBUTE,
                new SessionPrincipal(account.accountId(), account.displayName())
        );
        prepare(request);
    }

    private byte[] randomBytes() {
        byte[] value = new byte[RANDOM_VALUE_BYTES];
        secureRandom.nextBytes(value);
        return value;
    }
}
