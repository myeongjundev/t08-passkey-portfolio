package dev.myeongjun.passkey.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/** The minimum account identity kept in the server-side HTTP session. */
public record SessionPrincipal(UUID accountId, String displayName) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
