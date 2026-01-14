package service;

import config.AppConfig;
import exception.ClickLimitExceededException;
import model.ShortLink;
import model.UserProfile;
import org.junit.jupiter.api.Test;
import repository.FileJsonShortLinkRepository;
import repository.FileJsonUserRepository;
import repository.ShortLinkRepository;
import repository.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class UrlShortenerIntegrationIT {

    @Test
    void endToEndScenarioAccordingToSpec() throws Exception {
        Path linksFile = Files.createTempFile("src/db/links-it-", ".json");
        Path usersFile = Files.createTempFile("src/db/users-it-", ".json");

        AppConfig config = new AppConfig(
                Duration.ofHours(24),
                6,
                1,
                1000,
                2,                     // default maxClicks
                linksFile.toString(),
                usersFile.toString(),
                "clck.it"
        );

        ShortLinkRepository linkRepo = new FileJsonShortLinkRepository(linksFile.toString());
        UserRepository userRepo = new FileJsonUserRepository(usersFile.toString());

        UserService userService = new UserService(userRepo, config);
        UrlShortenerService urlService = new UrlShortenerService(linkRepo, config, userRepo);

        // 1. создаём пользователя
        UserProfile user = userService.createNewUser();

        // 2. создаём короткую ссылку
        ShortLink shortLink = urlService.createShortLink(user.getId(), "https://example.com/path");
        String code = shortLink.getShortCode();

        // 3. проверяем, что переход работает
        String resolved1 = urlService.resolveShortLink(code);
        assertEquals("https://example.com/path", resolved1);

        // 4. default maxClicks = 2, значит второй переход ок, третий — блок
        String resolved2 = urlService.resolveShortLink(code);
        assertEquals("https://example.com/path", resolved2);

        assertThrows(ClickLimitExceededException.class,
                () -> urlService.resolveShortLink(code),
                "После достижения лимита ссылка должна блокироваться");

        // 5. проверяем, что ссылка видна в списке ссылок пользователя
        assertEquals(1, urlService.getUserLinks(user.getId()).size());

        // 6. перезапускаем репозитории и сервисы, чтобы убедиться в персистентности
        ShortLinkRepository linkRepo2 = new FileJsonShortLinkRepository(linksFile.toString());
        UserRepository userRepo2 = new FileJsonUserRepository(usersFile.toString());
        UrlShortenerService urlService2 = new UrlShortenerService(linkRepo2, config, userRepo2);

        // ссылка всё ещё есть, но переход по ней уже заблокирован
        assertThrows(ClickLimitExceededException.class,
                () -> urlService2.resolveShortLink(code));
    }
}
