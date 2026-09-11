package dev.myeongjun.passkey.credential;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Says where a passkey lives, in the words the assignment asks for (T08-C26).
 *
 * <p>The registration response already carries both signals and they are already
 * persisted; nothing here queries the authenticator again. The backup-eligible flag
 * separates a synced credential -- Google Password Manager, iCloud Keychain -- from one
 * bound to a single device, and the transports separate this device from a phone reached
 * by QR or a security key. Neither is a secret: they describe the authenticator, not the
 * key material.
 */
public record PasskeyStorage(boolean synced, String label) {

    public static PasskeyStorage describe(boolean backupEligible, String transports) {
        Set<String> values = values(transports);

        // A security key is the whole answer; nothing else about it needs saying.
        if (values.contains("usb") || values.contains("nfc") || values.contains("ble")) {
            return new PasskeyStorage(backupEligible, "보안 키");
        }

        // Android reports internal *and* hybrid for one Google Password Manager passkey:
        // it was made here and can also be reached from elsewhere by QR. Where it was made
        // is the more useful half when someone is telling two rows apart.
        boolean here = values.contains("internal");
        boolean elsewhere = !here && values.contains("hybrid");

        if (backupEligible) {
            if (here) {
                return new PasskeyStorage(true, "동기화됨 · 이 기기");
            }
            if (elsewhere) {
                return new PasskeyStorage(true, "동기화됨 · 다른 기기");
            }
            return new PasskeyStorage(true, "동기화됨");
        }
        // "이 기기 전용" already names the device, so no carrier is appended to it.
        return new PasskeyStorage(false, elsewhere ? "다른 기기 전용" : "이 기기 전용");
    }

    private static Set<String> values(String transports) {
        if (transports == null || transports.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(transports.split(","))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }
}
