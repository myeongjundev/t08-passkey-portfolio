package dev.myeongjun.passkey.webauthn;

import com.webauthn4j.converter.util.ObjectConverter;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RegistrationCredentialFixture {

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private RegistrationCredentialFixture() {
    }

    public static String create(
            ObjectConverter converter,
            byte[] challenge,
            String rpId,
            String origin
    ) {
        return createBundle(converter, challenge, rpId, origin).registrationJson();
    }

    public static CredentialBundle createBundle(
            ObjectConverter converter,
            byte[] challenge,
            String rpId,
            String origin
    ) {
        try {
            byte[] credentialId = randomCredentialId();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair keyPair = generator.generateKeyPair();
            byte[] clientData = converter.getJsonMapper().writeValueAsBytes(Map.of(
                    "type", "webauthn.create",
                    "challenge", BASE64_URL.encodeToString(challenge),
                    "origin", origin,
                    "crossOrigin", false
            ));
            byte[] attestationObject = converter.getCborMapper().writeValueAsBytes(Map.of(
                    "fmt", "none",
                    "attStmt", Map.of(),
                    "authData", authenticatorData(converter, rpId, credentialId, (ECPublicKey) keyPair.getPublic())
            ));
            String encodedCredentialId = BASE64_URL.encodeToString(credentialId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", encodedCredentialId);
            response.put("rawId", encodedCredentialId);
            response.put("type", "public-key");
            response.put("authenticatorAttachment", "platform");
            response.put("clientExtensionResults", Map.of());
            response.put("response", Map.of(
                    "clientDataJSON", BASE64_URL.encodeToString(clientData),
                    "attestationObject", BASE64_URL.encodeToString(attestationObject),
                    "transports", List.of("internal")
            ));
            return new CredentialBundle(
                    converter.getJsonMapper().writeValueAsString(response), credentialId, keyPair.getPrivate());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static byte[] authenticatorData(
            ObjectConverter converter,
            String rpId,
            byte[] credentialId,
            ECPublicKey publicKey
    ) throws Exception {
        Map<Integer, Object> coseKey = new LinkedHashMap<>();
        coseKey.put(1, 2);
        coseKey.put(3, -7);
        coseKey.put(-1, 1);
        coseKey.put(-2, unsigned32(publicKey.getW().getAffineX()));
        coseKey.put(-3, unsigned32(publicKey.getW().getAffineY()));
        byte[] encodedCoseKey = converter.getCborMapper().writeValueAsBytes(coseKey);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(MessageDigest.getInstance("SHA-256").digest(rpId.getBytes(StandardCharsets.UTF_8)));
        output.write(0x45); // user present, user verified, attested credential data included
        output.write(new byte[4]);
        output.write(new byte[16]);
        output.write(ByteBuffer.allocate(2).putShort((short) credentialId.length).array());
        output.write(credentialId);
        output.write(encodedCoseKey);
        return output.toByteArray();
    }

    private static byte[] randomCredentialId() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        UUID value = UUID.randomUUID();
        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());
        return buffer.array();
    }

    private static byte[] unsigned32(BigInteger value) {
        byte[] source = value.toByteArray();
        byte[] result = new byte[32];
        int sourceStart = Math.max(0, source.length - result.length);
        int length = Math.min(source.length, result.length);
        System.arraycopy(source, sourceStart, result, result.length - length, length);
        return result;
    }

    public record CredentialBundle(String registrationJson, byte[] credentialId, PrivateKey privateKey) {
        public String authenticationJson(ObjectConverter converter,
                                         byte[] challenge,
                                         String rpId,
                                         String origin,
                                         byte[] userHandle,
                                         long signCount,
                                         boolean validSignature) {
            try {
                byte[] clientData = converter.getJsonMapper().writeValueAsBytes(Map.of(
                        "type", "webauthn.get",
                        "challenge", BASE64_URL.encodeToString(challenge),
                        "origin", origin,
                        "crossOrigin", false
                ));
                ByteArrayOutputStream authData = new ByteArrayOutputStream();
                authData.write(MessageDigest.getInstance("SHA-256")
                        .digest(rpId.getBytes(StandardCharsets.UTF_8)));
                authData.write(0x05);
                authData.write(ByteBuffer.allocate(4).putInt((int) signCount).array());

                ByteArrayOutputStream signed = new ByteArrayOutputStream();
                signed.write(authData.toByteArray());
                signed.write(MessageDigest.getInstance("SHA-256").digest(clientData));
                Signature signer = Signature.getInstance("SHA256withECDSA");
                signer.initSign(privateKey);
                signer.update(signed.toByteArray());
                byte[] signature = signer.sign();
                if (!validSignature) signature[signature.length - 1] ^= 1;

                String encodedId = BASE64_URL.encodeToString(credentialId);
                return converter.getJsonMapper().writeValueAsString(Map.of(
                        "id", encodedId,
                        "rawId", encodedId,
                        "type", "public-key",
                        "clientExtensionResults", Map.of(),
                        "response", Map.of(
                                "clientDataJSON", BASE64_URL.encodeToString(clientData),
                                "authenticatorData", BASE64_URL.encodeToString(authData.toByteArray()),
                                "signature", BASE64_URL.encodeToString(signature),
                                "userHandle", BASE64_URL.encodeToString(userHandle)
                        )
                ));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
