package dev.myeongjun.passkey.privatearea;

public class PrivateItemNotFoundException extends RuntimeException {
    public PrivateItemNotFoundException() {
        super("Private item not found");
    }
}
