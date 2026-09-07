package dev.myeongjun.passkey.webauthn;

public class AuthenticationVerificationException extends RuntimeException {
    public AuthenticationVerificationException(Throwable cause) {
        super("Authentication verification failed", cause);
    }
}
