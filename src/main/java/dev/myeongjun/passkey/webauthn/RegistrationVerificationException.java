package dev.myeongjun.passkey.webauthn;

public class RegistrationVerificationException extends RuntimeException {

    public RegistrationVerificationException(Throwable cause) {
        super("Passkey registration could not be verified", cause);
    }
}
