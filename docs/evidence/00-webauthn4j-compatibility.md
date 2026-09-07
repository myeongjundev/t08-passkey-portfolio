# WebAuthn4J compatibility spike

Checked locally on 2026-09-07 before Card 2 implementation.

```text
Spring Boot                  4.1.1
Java                         25
WebAuthn4J core              0.31.8.RELEASE
Jackson databind             tools.jackson.core:jackson-databind:3.1.5
WebAuthn4J requested Jackson tools 3.1.4; dependency management selected 3.1.5
```

`WebAuthnConfiguration` creates the official `WebAuthnManager`, and
`WebAuthnVerificationAdapter` compiles both registration and authentication JSON
parsing paths. The Spring integration test starts the full context and resolves the
adapter bean.

```text
./gradlew.bat clean test build -> BUILD SUCCESSFUL
```

This is dependency compatibility evidence, not proof of a successful passkey
ceremony. Registration verification, server-held challenges and persistence remain
Card 2 work.
