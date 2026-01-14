package repository;

import model.ShortLink;
import model.UserProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для файловых репозиториев:
 *  - FileJsonUserRepository
 *  - FileJsonShortLinkRepository
 * Здесь мы проверяем:
 *  - чтение/запись JSON
 *  - поиск по shortCode и ownerId
 *  - deleteExpired для ссылок
 */
public class FileJsonRepositoriesTest {

    @Test
    void userRepositorySaveAndFindWorksWithJsonFile() throws Exception {
        Path tempFile = Files.createTempFile("users-test-", ".json");

        FileJsonUserRepository repo = new FileJsonUserRepository(tempFile.toString());

        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId, 20, 12);
        repo.save(profile);

        FileJsonUserRepository repo2 = new FileJsonUserRepository(tempFile.toString());
        var loaded = repo2.findById(userId);

        assertTrue(loaded.isPresent(), "Пользователь должен быть найден после сохранения в JSON");
        assertEquals(20, loaded.get().getDefaultMaxClicks());
        assertEquals(12, loaded.get().getTtlHours());
    }

    @Test
    void shortLinkRepositorySaveAndFindByShortCodeAndOwner() throws Exception {
        Path tempFile = Files.createTempFile("links-test-", ".json");

        FileJsonShortLinkRepository repo = new FileJsonShortLinkRepository(tempFile.toString());

        UUID ownerId = UUID.randomUUID();
        ShortLink link = new ShortLink(
            UUID.randomUUID().toString(),
            "AbC123",
            "https://example.com",
            ownerId,
            10,
            0,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            true
        );

        repo.save(link);

        var byCode = repo.findByShortCode("AbC123");
        assertTrue(byCode.isPresent(), "Ссылка должна находиться по shortCode");
        assertEquals("https://example.com", byCode.get().getOriginalUrl());

        List<ShortLink> byOwner = repo.findByOwner(ownerId);
        assertEquals(1, byOwner.size(), "У владельца должна быть 1 ссылка");
    }

    @Test
    void shortLinkRepositoryDeleteByIdRemovesLink() throws Exception {
        Path tempFile = Files.createTempFile("links-del-test-", ".json");

        FileJsonShortLinkRepository repo = new FileJsonShortLinkRepository(tempFile.toString());

        UUID ownerId = UUID.randomUUID();
        ShortLink link = new ShortLink(
            UUID.randomUUID().toString(),
            "Del001",
            "https://delete.me",
            ownerId,
            5,
            0,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            true
        );
        repo.save(link);

        assertTrue(repo.findByShortCode("Del001").isPresent());

        repo.deleteById(link.getId());

        assertTrue(repo.findByShortCode("Del001").isEmpty(),
            "После deleteById ссылка не должна находиться в репозитории");
    }

    @Test
    void shortLinkRepositoryDeleteExpiredRemovesOnlyExpiredLinks() throws Exception {
        Path tempFile = Files.createTempFile("links-expired-test-", ".json");

        FileJsonShortLinkRepository repo = new FileJsonShortLinkRepository(tempFile.toString());

        UUID ownerId = UUID.randomUUID();
        ShortLink active = new ShortLink(
            UUID.randomUUID().toString(),
            "Act001",
            "https://alive.com",
            ownerId,
            5,
            0,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            true
        );
        ShortLink expired = new ShortLink(
            UUID.randomUUID().toString(),
            "Exp001",
            "https://dead.com",
            ownerId,
            5,
            0,
            Instant.now().minusSeconds(7200),
            Instant.now().minusSeconds(10),
            false
        );

        repo.save(active);
        repo.save(expired);

        // до очистки обе ссылки доступны
        assertTrue(repo.findByShortCode("Act001").isPresent());
        assertTrue(repo.findByShortCode("Exp001").isPresent());

        repo.deleteExpired(Instant.now());

        assertTrue(repo.findByShortCode("Act001").isPresent(),
            "Живая ссылка не должна быть удалена");
        assertTrue(repo.findByShortCode("Exp001").isEmpty(),
            "Просроченная ссылка должна быть удалена");
    }

    @Test
    void shortCodeExistsReturnsTrueIfShortCodeAlreadyUsed() throws Exception {
        Path tempFile = Files.createTempFile("links-exists-test-", ".json");

        FileJsonShortLinkRepository repo = new FileJsonShortLinkRepository(tempFile.toString());

        UUID ownerId = UUID.randomUUID();
        ShortLink link = new ShortLink(
            UUID.randomUUID().toString(),
            "CodeX1",
            "https://exists.com",
            ownerId,
            5,
            0,
            Instant.now(),
            Instant.now().plusSeconds(3600),
            true
        );
        repo.save(link);

        assertTrue(repo.shortCodeExists("CodeX1"),
            "shortCodeExists должен возвращать true для уже существующего кода");
        assertFalse(repo.shortCodeExists("Another"),
            "shortCodeExists должен возвращать false для несуществующего кода");
    }
}
