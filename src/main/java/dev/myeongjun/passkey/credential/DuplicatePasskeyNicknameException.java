package dev.myeongjun.passkey.credential;

/**
 * Raised when an account already has a passkey under the requested nickname.
 *
 * <p>Nicknames are unique per account (`uq_passkey_account_nickname`). Without this
 * check the collision would only surface as a constraint violation at save time —
 * after the browser ceremony has already created the credential on the user's
 * device, leaving a passkey the server never recorded.
 */
public class DuplicatePasskeyNicknameException extends RuntimeException {
    public DuplicatePasskeyNicknameException() {
        super("This account already has a passkey with that nickname");
    }
}
