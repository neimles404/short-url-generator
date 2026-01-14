package repository;

import model.ShortLink;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FileJsonShortLinkRepositoryIT {

    @Test
    void saveAndReloadShortLinksFromJson() throws Exception {
        Path tempFile = Files.createTempFile("links-", ".json");
        FileJsonShortLinkRepository repo = new FileJsonShortLinkRepository(tempFile.toString());

        UUID owner = UUID.randomUUID();
        ShortLink link = new ShortLink(
                UUID.randomUUID().toString(),
                "AbCd12",
                "https://example.com",
                owner,
                10,
                0,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                true
        );

        repo.save(link);

        // новый экземпляр репозитория, читающий тот же файл
        FileJsonShortLinkRepository repo2 = new FileJsonShortLinkRepository(tempFile.toString());
        var byCode = repo2.findByShortCode("AbCd12");
        assertTrue(byCode.isPresent());
        assertEquals("https://example.com", byCode.get().getOriginalUrl());

        List<ShortLink> byOwner = repo2.findByOwner(owner);
        assertEquals(1, byOwner.size());
    }

    @Test
    void deleteExpiredRemovesLinks() throws Exception {
        Path tempFile = Files.createTempFile("links-exp-", ".json");
        FileJsonShortLinkRepository repo = new FileJsonShortLinkRepository(tempFile.toString());

        UUID owner = UUID.randomUUID();
        ShortLink expired = new ShortLink(
                UUID.randomUUID().toString(),
                "Old123",
                "https://old.com",
                owner,
                10,
                0,
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(10),
                true
        );
        repo.save(expired);

        repo.deleteExpired(Instant.now());

        assertTrue(repo.findByShortCode("Old123").isEmpty(),
                "Протухшая ссылка должна быть удалена");
    }
}
