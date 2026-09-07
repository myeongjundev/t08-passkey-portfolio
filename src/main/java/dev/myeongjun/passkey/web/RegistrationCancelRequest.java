package dev.myeongjun.passkey.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegistrationCancelRequest(@NotNull UUID ceremonyId) {
}
