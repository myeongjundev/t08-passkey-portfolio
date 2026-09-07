package dev.myeongjun.passkey.webauthn;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Keeps the application-facing seam small while WebAuthn4J owns parsing and
 * cryptographic verification. Verification methods are added with Card 2/3 when
 * their server-held challenge parameters exist.
 */
@Component
public class WebAuthnVerificationAdapter {

    private final WebAuthnManager webAuthnManager;
    private final ObjectConverter objectConverter;
    private final WebAuthnProperties properties;

    public WebAuthnVerificationAdapter(
            WebAuthnManager webAuthnManager,
            ObjectConverter objectConverter,
            WebAuthnProperties properties
    ) {
        this.webAuthnManager = webAuthnManager;
        this.objectConverter = objectConverter;
        this.properties = properties;
    }

    public RegistrationData parseRegistrationResponse(String credentialJson) {
        return webAuthnManager.parseRegistrationResponseJSON(credentialJson);
    }

    public AuthenticationData parseAuthenticationResponse(String credentialJson) throws DataConversionException {
        return webAuthnManager.parseAuthenticationResponseJSON(credentialJson);
    }

    public VerifiedRegistration verifyRegistration(String credentialJson, byte[] expectedChallenge) {
        try {
            RegistrationData data = webAuthnManager.parseRegistrationResponseJSON(credentialJson);
            ServerProperty serverProperty = new ServerProperty(
                    new Origin(properties.origin().toString()),
                    properties.rpId(),
                    new DefaultChallenge(expectedChallenge),
                    null
            );
            List<PublicKeyCredentialParameters> algorithms = List.of(
                    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
                    new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256)
            );
            webAuthnManager.verify(data, new RegistrationParameters(serverProperty, algorithms, true, true));

            AuthenticatorData<?> authenticatorData = data.getAttestationObject().getAuthenticatorData();
            AttestedCredentialData attested = authenticatorData.getAttestedCredentialData();
            if (attested == null || !attested.getCOSEKey().hasPublicKey() || attested.getCOSEKey().hasPrivateKey()) {
                throw new IllegalArgumentException("registration did not contain a public-only credential key");
            }

            byte[] publicKeyCose = objectConverter.getCborMapper().writeValueAsBytes(attested.getCOSEKey());
            String metadata = objectConverter.getJsonMapper().writeValueAsString(Map.of(
                    "formatVersion", 1,
                    "algorithm", attested.getCOSEKey().getAlgorithm().getValue()
            ));
            String transports = Optional.ofNullable(data.getTransports()).orElse(Set.of()).stream()
                    .map(transport -> transport.getValue())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(","));

            return new VerifiedRegistration(
                    attested.getCredentialId(),
                    publicKeyCose,
                    metadata,
                    authenticatorData.getSignCount(),
                    transports,
                    authenticatorData.isFlagBE(),
                    authenticatorData.isFlagBS()
            );
        } catch (Exception exception) {
            throw new RegistrationVerificationException(exception);
        }
    }

    public byte[] authenticationCredentialId(String credentialJson) {
        try {
            return webAuthnManager.parseAuthenticationResponseJSON(credentialJson).getCredentialId();
        } catch (Exception exception) {
            throw new AuthenticationVerificationException(exception);
        }
    }

    public VerifiedAuthentication verifyAuthentication(
            String credentialJson,
            byte[] expectedChallenge,
            StoredAuthenticator stored
    ) {
        try {
            AuthenticationData data = webAuthnManager.parseAuthenticationResponseJSON(credentialJson);
            if (!Arrays.equals(data.getCredentialId(), stored.credentialId())) {
                throw new IllegalArgumentException("credential id mismatch");
            }
            COSEKey coseKey = objectConverter.getCborMapper()
                    .readValue(stored.publicKeyCose(), COSEKey.class);
            Set<AuthenticatorTransport> transports = stored.transports() == null || stored.transports().isBlank()
                    ? Set.of()
                    : Arrays.stream(stored.transports().split(","))
                    .map(AuthenticatorTransport::create)
                    .collect(Collectors.toUnmodifiableSet());
            AuthenticatorImpl authenticator = new AuthenticatorImpl(
                    new AttestedCredentialData(AAGUID.ZERO, stored.credentialId(), coseKey),
                    new NoneAttestationStatement(),
                    stored.signCount(),
                    transports
            );
            ServerProperty serverProperty = new ServerProperty(
                    new Origin(properties.origin().toString()),
                    properties.rpId(),
                    new DefaultChallenge(expectedChallenge),
                    null
            );
            webAuthnManager.verify(data, new AuthenticationParameters(
                    serverProperty, authenticator, null, true, true));
            return new VerifiedAuthentication(
                    data.getUserHandle(),
                    data.getAuthenticatorData().getSignCount(),
                    data.getAuthenticatorData().isFlagBS()
            );
        } catch (Exception exception) {
            throw new AuthenticationVerificationException(exception);
        }
    }
}
