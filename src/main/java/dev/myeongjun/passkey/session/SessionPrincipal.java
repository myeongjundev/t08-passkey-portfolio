package dev.myeongjun.passkey.session;

import java.util.UUID;

/** The minimum account identity kept in the server-side HTTP session. */
public record SessionPrincipal(UUID accountId, String displayName) {
}
