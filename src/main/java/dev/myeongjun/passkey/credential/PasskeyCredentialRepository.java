package dev.myeongjun.passkey.credential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, UUID> {

    Optional<PasskeyCredential> findByCredentialId(byte[] credentialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select credential from PasskeyCredential credential where credential.credentialId = :credentialId")
    Optional<PasskeyCredential> findByCredentialIdForUpdate(@Param("credentialId") byte[] credentialId);

    List<PasskeyCredential> findAllByAccountIdOrderByRegisteredAt(UUID accountId);

    boolean existsByAccountIdAndNickname(UUID accountId, String nickname);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select credential from PasskeyCredential credential where credential.accountId = :accountId order by credential.registeredAt")
    List<PasskeyCredential> findAllByAccountIdForUpdate(@Param("accountId") UUID accountId);
}
