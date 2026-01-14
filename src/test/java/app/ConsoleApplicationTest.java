package app;

import config.AppConfig;
import model.UserProfile;
import repository.ShortLinkRepository;
import repository.UserRepository;
import service.ExpirationCleanupService;
import service.UrlShortenerService;
import service.UserService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Простые интеграционные тесты для ConsoleApplication.
 * Здесь мы не проверяем всю бизнес-логику (она тестируется в сервисах),
 * а только то, что приложение:
 *  - корректно стартует,
 *  - может завершиться через меню,
 *  - не падает при базовых сценариях.
 */
public class ConsoleApplicationTest {

    private InputStream originalIn;
    private PrintStream originalOut;

    private ByteArrayOutputStream testOut;

    // In-memory реализации репозиториев, чтобы не трогать настоящие JSON-файлы
    static class InMemoryShortLinkRepository implements ShortLinkRepository {
        private final Map<String, model.ShortLink> storage = new HashMap<>();

        @Override
        public void save(model.ShortLink link) {
            storage.put(link.getId(), link);
        }

        @Override
        public Optional<model.ShortLink> findByShortCode(String shortCode) {
            return storage.values().stream()
                .filter(l -> l.getShortCode().equals(shortCode))
                .findFirst();
        }

        @Override
        public java.util.List<model.ShortLink> findByOwner(UUID ownerId) {
            return storage.values().stream()
                .filter(l -> l.getOwnerId().equals(ownerId))
                .toList();
        }

        @Override
        public void deleteById(String id) {
            storage.remove(id);
        }

        @Override
        public void deleteExpired(java.time.Instant now) {
            var toRemove = storage.values().stream()
                .filter(l -> l.isExpired(now))
                .map(model.ShortLink::getId)
                .toList();
            toRemove.forEach(storage::remove);
        }

        @Override
        public boolean shortCodeExists(String shortCode) {
            return storage.values().stream()
                .anyMatch(l -> l.getShortCode().equals(shortCode));
        }
    }

    static class InMemoryUserRepository implements UserRepository {
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

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;

        testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    private AppConfig createTestConfig() {
        return new AppConfig(
            Duration.ofHours(24),
            6,
            1,
            1000,
            10,
            "src/db/test-links.json",
            "src/db/test-users.json",
            "clck.test"
        );
    }

    @Test
    void applicationExitsImmediatelyWhenUserChoosesZero() {
        // Сценарий: пользователь сразу вводит 0 на первом экране → приложение завершается
        String input = "0\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        AppConfig config = createTestConfig();
        InMemoryShortLinkRepository linkRepo = new InMemoryShortLinkRepository();
        InMemoryUserRepository userRepo = new InMemoryUserRepository();

        UserService userService = new UserService(userRepo, config);
        UrlShortenerService urlService = new UrlShortenerService(linkRepo, config, userRepo);

        ExpirationCleanupService cleanupService = new ExpirationCleanupService(urlService, 1000);
        cleanupService.start();

        ConsoleApplication app = new ConsoleApplication(urlService, userService, config);
        app.run();

        cleanupService.stop();

        String output = testOut.toString();
        assertTrue(output.contains("Выход из приложения") || output.contains("Выход"),
            "Ожидалось, что приложение сообщит о выходе.");
    }

    @Test
    void applicationCreatesNewUserAndThenExits() {
        // Сценарий:
        //  2 -> создать нового пользователя
        //  0 -> в главном меню, затем выход
        String input = String.join("\n",
            "2",   // создать пользователя
            "0"    // в главном меню – выход
        ) + "\n";

        System.setIn(new ByteArrayInputStream(input.getBytes()));

        AppConfig config = createTestConfig();
        InMemoryShortLinkRepository linkRepo = new InMemoryShortLinkRepository();
        InMemoryUserRepository userRepo = new InMemoryUserRepository();

        UserService userService = new UserService(userRepo, config);
        UrlShortenerService urlService = new UrlShortenerService(linkRepo, config, userRepo);

        ExpirationCleanupService cleanupService = new ExpirationCleanupService(urlService, 1000);
        cleanupService.start();

        ConsoleApplication app = new ConsoleApplication(urlService, userService, config);
        app.run();

        cleanupService.stop();

        String output = testOut.toString();
        assertTrue(output.contains("Создан новый пользователь"),
            "Ожидалось сообщение о создании нового пользователя.");
    }
}
