package dev.myeongjun.passkey.privatearea;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PrivateAreaService {

    private final PrivateItemRepository privateItemRepository;

    public PrivateAreaService(PrivateItemRepository privateItemRepository) {
        this.privateItemRepository = privateItemRepository;
    }

    public List<PrivateItemView> findForAccount(UUID accountId) {
        return privateItemRepository.findAllByAccountIdOrderBySortOrder(accountId).stream()
                .map(item -> new PrivateItemView(item.title(), item.body()))
                .toList();
    }

    public PrivateItemView findOneForAccount(UUID accountId, UUID itemId) {
        PrivateItem item = privateItemRepository.findByIdAndAccountId(itemId, accountId)
                .orElseThrow(PrivateItemNotFoundException::new);
        return new PrivateItemView(item.title(), item.body());
    }
}
