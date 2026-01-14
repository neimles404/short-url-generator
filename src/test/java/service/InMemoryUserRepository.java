package service;

import model.UserProfile;
import repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryUserRepository implements UserRepository {

    private final Map<UUID, UserProfile> storage = new HashMap<>();

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public void save(UserProfile user) {
        storage.put(user.getId(), user);
    }
}
