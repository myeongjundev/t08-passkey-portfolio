package dev.myeongjun.passkey.credential;

public class PasskeyNotFoundException extends RuntimeException {
    public PasskeyNotFoundException() {
        super("Passkey not found");
    }
}
