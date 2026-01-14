package service;

import model.ShortLink;
import repository.ShortLinkRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryShortLinkRepository implements ShortLinkRepository {

    private final Map<String, ShortLink> storage = new HashMap<>();

    @Override
    public void save(ShortLink link) {
        storage.put(link.getId(), link);
    }

    @Override
    public Optional<ShortLink> findByShortCode(String shortCode) {
        return storage.values().stream()
                .filter(l -> l.getShortCode().equals(shortCode))
                .findFirst();
    }

    @Override
    public List<ShortLink> findByOwner(UUID ownerId) {
        return storage.values().stream()
                .filter(l -> l.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        storage.remove(id);
    }

    @Override
    public void deleteExpired(Instant now) {
        List<String> toRemove = storage.values().stream()
                .filter(l -> l.isExpired(now))
                .map(ShortLink::getId)
                .toList();
        toRemove.forEach(storage::remove);
    }

    @Override
    public boolean shortCodeExists(String shortCode) {
        return storage.values().stream()
                .anyMatch(l -> l.getShortCode().equals(shortCode));
    }

    public int size() {
        return storage.size();
    }
}
