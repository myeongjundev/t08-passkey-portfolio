package dev.myeongjun.passkey.web;

import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.credential.AddPasskeyOptions;
import dev.myeongjun.passkey.credential.PasskeyManagementService;
import dev.myeongjun.passkey.credential.PasskeyView;
import dev.myeongjun.passkey.session.SessionAuthentication;
import dev.myeongjun.passkey.session.SessionPrincipal;
import dev.myeongjun.passkey.session.SessionSecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/passkeys")
public class PasskeyController {

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final PasskeyManagementService passkeyService;
    private final SessionSecurityContext sessionSecurityContext;
    private final WebAuthnProperties properties;

    public PasskeyController(PasskeyManagementService passkeyService,
                             SessionSecurityContext sessionSecurityContext,
                             WebAuthnProperties properties) {
        this.passkeyService = passkeyService;
        this.sessionSecurityContext = sessionSecurityContext;
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<List<PasskeyView>> list(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(passkeyService.list(principal(request).accountId()));
    }

    @PostMapping("/options")
    public ResponseEntity<RegistrationOptionsResponse> options(
            @Valid @RequestBody PasskeyOptionsRequest body,
            HttpServletRequest request
    ) {
        SessionPrincipal principal = principal(request);
        AddPasskeyOptions options = passkeyService.issueOptions(
                principal.accountId(), sessionSecurityContext.ownerKey(request), body.nickname());
        List<Object> excluded = options.excludedCredentialIds().stream()
                .map(id -> (Object) Map.of("type", "public-key", "id", BASE64_URL.encodeToString(id)))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new RegistrationOptionsResponse(
                        options.ceremony().id(),
                        new RegistrationOptionsResponse.PublicKeyCreationOptions(
                                BASE64_URL.encodeToString(options.ceremony().challenge()),
                                new RegistrationOptionsResponse.RelyingParty(properties.rpId(), properties.rpName()),
                                new RegistrationOptionsResponse.User(
                                        BASE64_URL.encodeToString(options.userHandle()),
                                        "account-" + principal.accountId(),
                                        options.displayName()),
                                List.of(
                                        new RegistrationOptionsResponse.CredentialParameter("public-key", -7),
                                        new RegistrationOptionsResponse.CredentialParameter("public-key", -257)),
                                properties.timeout().toMillis(),
                                new RegistrationOptionsResponse.AuthenticatorSelection("required", true, "required"),
                                "none",
                                excluded
                        )
                ));
    }

    @PostMapping("/finish")
    public ResponseEntity<PasskeyView> finish(
            @Valid @RequestBody RegistrationFinishRequest body,
            HttpServletRequest request
    ) {
        SessionPrincipal principal = principal(request);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(passkeyService.finish(
                        principal.accountId(), body.ceremonyId(),
                        sessionSecurityContext.ownerKey(request), body.credential().toString()));
    }

    @DeleteMapping("/{passkeyId}")
    public ResponseEntity<Void> delete(@PathVariable UUID passkeyId, HttpServletRequest request) {
        passkeyService.delete(principal(request).accountId(), passkeyId);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private SessionPrincipal principal(HttpServletRequest request) {
        return SessionAuthentication.current(request.getSession(false)).orElseThrow();
    }
}
