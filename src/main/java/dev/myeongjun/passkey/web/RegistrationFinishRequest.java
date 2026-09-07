package dev.myeongjun.passkey.web;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record RegistrationFinishRequest(
        @NotNull UUID ceremonyId,
        @NotNull JsonNode credential
) {
}
