package service;

import config.AppConfig;
import exception.LinkNotFoundException;
import model.ShortLink;
import model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Дополнительные тесты для достижения полного покрытия и проверки устойчивости системы.
 * Всего в проекте теперь 15 тестов.
 */
public class AdditionalTests {

    private InMemoryShortLinkRepository linkRepo;
    private InMemoryUserRepository userRepo;
    private UrlShortenerService service;
    private AppConfig config;

    @BeforeEach
    void setup() {
        linkRepo = new InMemoryShortLinkRepository();
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
        service = new UrlShortenerService(linkRepo, config, userRepo);
    }

    @Test
    void appConfigValidationRejectsInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () ->
            new AppConfig(Duration.ZERO, 6, 1, 10, 5,
                "db.json", "users.json", "base"));
        assertThrows(IllegalArgumentException.class, () ->
            new AppConfig(Duration.ofHours(1), -1, 1, 10, 5,
                "db.json", "users.json", "base"));
        assertThrows(IllegalArgumentException.class, () ->
            new AppConfig(Duration.ofHours(1), 6, 0, -10, 5,
                "db.json", "users.json", "base"));
        assertThrows(IllegalArgumentException.class, () ->
            new AppConfig(Duration.ofHours(1), 6, 1, 10, 100,
                "db.json", "users.json", "base"));
        assertThrows(IllegalArgumentException.class, () ->
            new AppConfig(Duration.ofHours(1), 6, 1, 10, 5,
                "", "users.json", "base"));
    }

    @Test
    void deleteExpiredRemovesAllExpiredLinks() {
        UUID userId = UUID.randomUUID();
        userRepo.save(new UserProfile(userId, 10, 1));

        ShortLink active = new ShortLink(UUID.randomUUID().toString(), "ABC123",
            "https://alive.com", userId, 5, 0,
            Instant.now(), Instant.now().plus(Duration.ofHours(5)), true);

        ShortLink expired = new ShortLink(UUID.randomUUID().toString(), "OLD001",
            "https://old.com", userId, 5, 0,
            Instant.now().minus(Duration.ofDays(1)),
            Instant.now().minus(Duration.ofHours(1)), true);

        linkRepo.save(active);
        linkRepo.save(expired);

        assertEquals(2, linkRepo.size());

        linkRepo.deleteExpired(Instant.now());

        assertEquals(1, linkRepo.size(), "После очистки должен остаться только активный линк");
        assertTrue(linkRepo.findByShortCode("ABC123").isPresent());
        assertTrue(linkRepo.findByShortCode("OLD001").isEmpty());
    }

    @Test
    void userServiceThrowsIfUserNotFound() throws Exception {
        UserService userService = new UserService(userRepo, config);

        UUID fakeId = UUID.randomUUID();
        assertTrue(userService.findUser(fakeId).isEmpty());

        assertThrows(IllegalArgumentException.class,
            () -> userService.updateUserSettings(fakeId, 10, 10),
            "Должно бросать исключение при изменении несуществующего пользователя");
    }

    @Test
    void deletingNonexistentLinkThrowsLinkNotFound() {
        UUID userId = UUID.randomUUID();
        userRepo.save(new UserProfile(userId, 10, 1));

        assertThrows(LinkNotFoundException.class,
            () -> service.deleteUserLink(userId, "NON_EXISTENT"),
            "Удаление несуществующей ссылки должно приводить к LinkNotFoundException");
    }

    @Test
    void shortLinkExpirationAndClickLimitChecksWorkCorrectly() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        ShortLink link = new ShortLink(
            UUID.randomUUID().toString(),
            "XYZ111",
            "https://test.com",
            userId,
            3,
            2,
            now,
            now.plus(Duration.ofHours(2)), // живой
            true
        );

        assertFalse(link.isExpired(now));
        assertFalse(link.isClickLimitExceeded());

        // имитируем 3-й клик
        link.incrementClick();
        assertTrue(link.isClickLimitExceeded(), "После 3-го клика лимит должен быть исчерпан");

        // имитируем истечение срока
        assertTrue(link.isExpired(now.plus(Duration.ofHours(3))),
            "После истечения TTL ссылка должна считаться просроченной");
    }
}
