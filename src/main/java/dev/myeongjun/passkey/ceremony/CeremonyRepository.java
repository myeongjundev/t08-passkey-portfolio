package dev.myeongjun.passkey.ceremony;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CeremonyRepository extends JpaRepository<WebAuthnCeremony, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from WebAuthnCeremony c where c.id = :id")
    Optional<WebAuthnCeremony> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from WebAuthnCeremony c
            where c.ownerKey = :ownerKey
              and c.kind = :kind
              and c.activeSlot = 'ACTIVE'
              and c.consumedAt is null
            """)
    Optional<WebAuthnCeremony> findActiveForOwner(
            @Param("ownerKey") byte[] ownerKey,
            @Param("kind") CeremonyKind kind
    );

    long countByOwnerKeyAndKindAndActiveSlot(byte[] ownerKey, CeremonyKind kind, String activeSlot);
}
