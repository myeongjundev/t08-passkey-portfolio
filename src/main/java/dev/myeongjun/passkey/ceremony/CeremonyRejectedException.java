package dev.myeongjun.passkey.ceremony;

public class CeremonyRejectedException extends RuntimeException {

    private final CeremonyRejection reason;

    public CeremonyRejectedException(CeremonyRejection reason) {
        super("WebAuthn ceremony rejected");
        this.reason = reason;
    }

    public CeremonyRejection reason() {
        return reason;
    }
}
