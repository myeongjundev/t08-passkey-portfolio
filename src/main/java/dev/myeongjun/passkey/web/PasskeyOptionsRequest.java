package dev.myeongjun.passkey.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasskeyOptionsRequest(@NotBlank @Size(max = 40) String nickname) {
}
