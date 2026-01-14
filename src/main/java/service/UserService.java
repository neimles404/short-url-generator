package service;

import config.AppConfig;
import exception.DataAccessException;
import model.UserProfile;
import repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class UserService {

    private final UserRepository userRepository;
    private final AppConfig config;

    public UserService(UserRepository userRepository, AppConfig config) {
        this.userRepository = userRepository;
        this.config = config;
    }

    public UserProfile createNewUser() throws DataAccessException {
        UUID id = UUID.randomUUID();
        int defaultMaxClicks = config.defaultMaxClicks();
        long ttlHours = config.linkTtl().toHours();

        UserProfile user = new UserProfile(id, defaultMaxClicks, ttlHours);
        userRepository.save(user);
        return user;
    }

    public Optional<UserProfile> findUser(UUID id) throws DataAccessException {
        return userRepository.findById(id);
    }

    public UserProfile getRequiredUser(UUID id) throws DataAccessException {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    public UserProfile updateUserSettings(UUID id, int defaultMaxClicks, long ttlHours)
            throws DataAccessException {

        if (ttlHours <= 0) {
            throw new IllegalArgumentException("TTL (в часах) должен быть > 0");
        }
        if (defaultMaxClicks < config.minClicksAllowed()
                || defaultMaxClicks > config.maxClicksAllowed()) {
            throw new IllegalArgumentException("Лимит переходов должен быть в диапазоне " +
                    config.minClicksAllowed() + "–" + config.maxClicksAllowed());
        }

        UserProfile user = getRequiredUser(id);
        user.setDefaultMaxClicks(defaultMaxClicks);
        user.setTtlHours(ttlHours);
        userRepository.save(user);
        return user;
    }
}
