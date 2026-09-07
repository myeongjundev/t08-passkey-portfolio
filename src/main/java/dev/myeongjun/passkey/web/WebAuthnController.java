package dev.myeongjun.passkey.web;

import dev.myeongjun.passkey.ceremony.CeremonyOptions;
import dev.myeongjun.passkey.ceremony.CeremonyService;
import dev.myeongjun.passkey.ceremony.CeremonyKind;
import dev.myeongjun.passkey.account.AccountRegistrationService;
import dev.myeongjun.passkey.account.AccountAuthenticationService;
import dev.myeongjun.passkey.account.RegisteredAccount;
import dev.myeongjun.passkey.config.WebAuthnProperties;
import dev.myeongjun.passkey.session.SessionSecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/webauthn")
public class WebAuthnController {

    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final CeremonyService ceremonyService;
    private final AccountRegistrationService accountRegistrationService;
    private final AccountAuthenticationService accountAuthenticationService;
    private final SessionSecurityContext sessionSecurityContext;
    private final WebAuthnProperties properties;

    public WebAuthnController(
            CeremonyService ceremonyService,
            AccountRegistrationService accountRegistrationService,
            AccountAuthenticationService accountAuthenticationService,
            SessionSecurityContext sessionSecurityContext,
            WebAuthnProperties properties
    ) {
        this.ceremonyService = ceremonyService;
        this.accountRegistrationService = accountRegistrationService;
        this.accountAuthenticationService = accountAuthenticationService;
        this.sessionSecurityContext = sessionSecurityContext;
        this.properties = properties;
    }

    @PostMapping("/register/cancel")
    public ResponseEntity<Void> registrationCancel(
            @Valid @RequestBody RegistrationCancelRequest body,
            HttpServletRequest request
    ) {
        ceremonyService.consume(
                body.ceremonyId(),
                CeremonyKind.CREATE_ACCOUNT,
                sessionSecurityContext.ownerKey(request)
        );
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    @PostMapping("/register/finish")
    public ResponseEntity<RegistrationFinishResponse> registrationFinish(
            @Valid @RequestBody RegistrationFinishRequest body,
            HttpServletRequest request
    ) {
        RegisteredAccount account = accountRegistrationService.finish(
                body.ceremonyId(),
                sessionSecurityContext.ownerKey(request),
                body.credential().toString()
        );

        sessionSecurityContext.authenticate(request, account);

        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore())
                .body(new RegistrationFinishResponse("/private"));
    }

    @PostMapping("/authenticate/options")
    public ResponseEntity<AuthenticationOptionsResponse> authenticationOptions(HttpServletRequest request) {
        CeremonyOptions ceremony = ceremonyService.issueAuthentication(sessionSecurityContext.ownerKey(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AuthenticationOptionsResponse(
                        ceremony.id(),
                        new AuthenticationOptionsResponse.PublicKeyRequestOptions(
                                BASE64_URL.encodeToString(ceremony.challenge()),
                                properties.timeout().toMillis(),
                                properties.rpId(),
                                "required",
                                List.of()
                        )
                ));
    }

    @PostMapping("/authenticate/finish")
    public ResponseEntity<RegistrationFinishResponse> authenticationFinish(
            @Valid @RequestBody RegistrationFinishRequest body,
            HttpServletRequest request
    ) {
        RegisteredAccount account = accountAuthenticationService.finish(
                body.ceremonyId(),
                sessionSecurityContext.ownerKey(request),
                body.credential().toString()
        );
        sessionSecurityContext.authenticate(request, account);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new RegistrationFinishResponse("/private"));
    }

    @PostMapping("/register/options")
    public ResponseEntity<RegistrationOptionsResponse> registrationOptions(
            @Valid @RequestBody RegistrationOptionsRequest body,
            HttpServletRequest request
    ) {
        CeremonyOptions ceremony = ceremonyService.issueCreateAccount(
                sessionSecurityContext.ownerKey(request),
                body.displayName(),
                body.nickname()
        );

        RegistrationOptionsResponse response = new RegistrationOptionsResponse(
                ceremony.id(),
                new RegistrationOptionsResponse.PublicKeyCreationOptions(
                        BASE64_URL.encodeToString(ceremony.challenge()),
                        new RegistrationOptionsResponse.RelyingParty(properties.rpId(), properties.rpName()),
                        new RegistrationOptionsResponse.User(
                                BASE64_URL.encodeToString(ceremony.pendingUserHandle()),
                                "synthetic-" + ceremony.id(),
                                body.displayName().strip()
                        ),
                        List.of(
                                new RegistrationOptionsResponse.CredentialParameter("public-key", -7),
                                new RegistrationOptionsResponse.CredentialParameter("public-key", -257)
                        ),
                        properties.timeout().toMillis(),
                        new RegistrationOptionsResponse.AuthenticatorSelection("required", true, "required"),
                        "none",
                        List.of()
                )
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
