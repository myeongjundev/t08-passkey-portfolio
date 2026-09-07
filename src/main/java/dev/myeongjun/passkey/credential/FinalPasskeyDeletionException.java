package dev.myeongjun.passkey.credential;

public class FinalPasskeyDeletionException extends RuntimeException {
    public FinalPasskeyDeletionException() {
        super("Register another passkey before deleting the final one");
    }
}
