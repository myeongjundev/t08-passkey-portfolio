package dev.myeongjun.passkey.credential;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T08-C26 asks where the passkey is kept. The registration response already answers it
 * and the answer is already stored, so the list can say it instead of sending a person
 * into their phone's settings. These fix the wording that claim depends on.
 */
class PasskeyStorageTests {

    @Test
    void aSyncedCredentialOnThisDeviceSaysBoth() {
        PasskeyStorage storage = PasskeyStorage.describe(true, "internal,hybrid");

        assertThat(storage.synced()).isTrue();
        assertThat(storage.label()).isEqualTo("동기화됨 · 이 기기");
    }

    @Test
    void aDeviceBoundCredentialSaysItStaysHere() {
        PasskeyStorage storage = PasskeyStorage.describe(false, "internal");

        assertThat(storage.synced()).isFalse();
        assertThat(storage.label()).isEqualTo("이 기기 전용");
    }

    @Test
    void aSecurityKeyIsTheWholeAnswer() {
        assertThat(PasskeyStorage.describe(false, "usb,internal").label()).isEqualTo("보안 키");
        assertThat(PasskeyStorage.describe(false, "nfc").label()).isEqualTo("보안 키");
    }

    /** Android reports both for one Google Password Manager passkey. */
    @Test
    void whereItWasMadeWinsOverWhereItCanBeReached() {
        assertThat(PasskeyStorage.describe(true, "internal,hybrid").label()).isEqualTo("동기화됨 · 이 기기");
    }

    @Test
    void aDeviceBoundRowNeverRepeatsTheDevice() {
        assertThat(PasskeyStorage.describe(false, "internal").label()).isEqualTo("이 기기 전용");
        assertThat(PasskeyStorage.describe(false, "hybrid").label()).isEqualTo("다른 기기 전용");
    }

    @Test
    void aPhoneReachedByQrIsNotThisDevice() {
        assertThat(PasskeyStorage.describe(true, "hybrid").label()).isEqualTo("동기화됨 · 다른 기기");
    }

    @Test
    void missingTransportsStillSayWhetherItSyncs() {
        assertThat(PasskeyStorage.describe(true, null).label()).isEqualTo("동기화됨");
        assertThat(PasskeyStorage.describe(false, "").label()).isEqualTo("이 기기 전용");
        assertThat(PasskeyStorage.describe(false, "  ").label()).isEqualTo("이 기기 전용");
    }

    @Test
    void anUnknownTransportDoesNotInventACarrier() {
        assertThat(PasskeyStorage.describe(true, "smart-card").label()).isEqualTo("동기화됨");
    }
}
