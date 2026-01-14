package service;

import config.AppConfig;
import exception.DataAccessException;
import model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private InMemoryUserRepository userRepo;
    private AppConfig config;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepo = new InMemoryUserRepository();
        config = new AppConfig(
                Duration.ofHours(24),
                6,
                1,
                1000,
                10,
                "src/db/test-links.json",
                "src/db/test-users.json",
                "clck.test"
        );
        userService = new UserService(userRepo, config);
    }

    @Test
    void createNewUserUsesDefaultsFromConfig() throws DataAccessException {
        UserProfile user = userService.createNewUser();

        assertNotNull(user.getId());
        assertEquals(config.defaultMaxClicks(), user.getDefaultMaxClicks());
        assertEquals(config.linkTtl().toHours(), user.getTtlHours());
    }

    @Test
    void updateUserSettingsValidatesLimitAndTtl() throws DataAccessException {
        UserProfile user = userService.createNewUser();

        // корректное обновление
        UserProfile updated = userService.updateUserSettings(
                user.getId(),
                50,
                12
        );
        assertEquals(50, updated.getDefaultMaxClicks());
        assertEquals(12, updated.getTtlHours());

        // некорректный лимит
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserSettings(user.getId(), 0, 10));

        // некорректный ttl
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUserSettings(user.getId(), 10, 0));
    }

    @Test
    void findUserReturnsEmptyIfNotExists() throws DataAccessException {
        assertTrue(userService.findUser(UUID.randomUUID()).isEmpty());
    }
}
