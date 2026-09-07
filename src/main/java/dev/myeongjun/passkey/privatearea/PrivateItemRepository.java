package dev.myeongjun.passkey.privatearea;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PrivateItemRepository extends JpaRepository<PrivateItem, UUID> {

    List<PrivateItem> findAllByAccountIdOrderBySortOrder(UUID accountId);

    Optional<PrivateItem> findByIdAndAccountId(UUID id, UUID accountId);
}
