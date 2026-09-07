package dev.myeongjun.passkey.ceremony;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ChallengeFingerprint {

    private static final int DISPLAY_HEX_LENGTH = 12;

    private ChallengeFingerprint() {
    }

    public static String of(byte[] challenge) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(challenge);
            return HexFormat.of().formatHex(digest, 0, DISPLAY_HEX_LENGTH / 2);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
