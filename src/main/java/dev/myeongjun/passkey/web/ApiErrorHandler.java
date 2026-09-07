package dev.myeongjun.passkey.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import dev.myeongjun.passkey.ceremony.CeremonyRejectedException;
import dev.myeongjun.passkey.webauthn.RegistrationVerificationException;
import dev.myeongjun.passkey.webauthn.AuthenticationVerificationException;
import dev.myeongjun.passkey.credential.DuplicatePasskeyNicknameException;
import dev.myeongjun.passkey.credential.FinalPasskeyDeletionException;
import dev.myeongjun.passkey.credential.PasskeyNotFoundException;
import dev.myeongjun.passkey.privatearea.PrivateItemNotFoundException;

import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            CeremonyRejectedException.class,
            RegistrationVerificationException.class,
            AuthenticationVerificationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, String>> invalidInput() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "invalid_request"));
    }

    @ExceptionHandler(FinalPasskeyDeletionException.class)
    public ResponseEntity<Map<String, String>> finalPasskey() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "final_passkey_required"));
    }

    @ExceptionHandler(DuplicatePasskeyNicknameException.class)
    public ResponseEntity<Map<String, String>> duplicateNickname() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "nickname_taken"));
    }

    /**
     * Safety net so a unique-constraint race cannot reach the client as a 500.
     * The message is deliberately generic: constraint names describe the schema.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict"));
    }

    @ExceptionHandler({PasskeyNotFoundException.class, PrivateItemNotFoundException.class})
    public ResponseEntity<Map<String, String>> passkeyNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "resource_not_found"));
    }
}
