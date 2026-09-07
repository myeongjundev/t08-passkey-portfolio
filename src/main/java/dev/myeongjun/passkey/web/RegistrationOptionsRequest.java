package dev.myeongjun.passkey.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationOptionsRequest(
        @NotBlank @Size(max = 40) String displayName,
        @NotBlank @Size(max = 40) String nickname
) {
}
