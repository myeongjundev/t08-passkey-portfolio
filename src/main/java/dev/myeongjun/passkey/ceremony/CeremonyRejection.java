package dev.myeongjun.passkey.ceremony;

public enum CeremonyRejection {
    NOT_FOUND,
    OWNER_MISMATCH,
    REPLAYED,
    EXPIRED,
    KIND_MISMATCH
}
