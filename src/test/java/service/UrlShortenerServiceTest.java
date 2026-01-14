package service;

import config.AppConfig;
import exception.*;
import model.ShortLink;
import model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UrlShortenerServiceTest {

    private InMemoryShortLinkRepository linkRepo;
    private InMemoryUserRepository userRepo;
    private UrlShortenerService service;
    private AppConfig config;

    @BeforeEach
    void setUp() {
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

    private UserProfile createUser(int maxClicks, long ttlHours) {
        UUID id = UUID.randomUUID();
        UserProfile u = new UserProfile(id, maxClicks, ttlHours);
        userRepo.save(u);
        return u;
    }

    @Test
    void sameUrlDifferentUsersProduceDifferentShortCodes() throws Exception {
        UserProfile u1 = createUser(10, 24);
        UserProfile u2 = createUser(10, 24);

        ShortLink l1 = service.createShortLink(u1.getId(), "https://example.com");
        ShortLink l2 = service.createShortLink(u2.getId(), "https://example.com");

        assertNotEquals(l1.getShortCode(), l2.getShortCode(),
                "Для разных пользователей короткие ссылки должны быть разными, даже если URL совпадает");
    }

    @Test
    void linkBlockedAfterClickLimit() throws Exception {
        UserProfile u = createUser(1, 24);
        ShortLink link = service.createShortLink(u.getId(), "https://example.com");

        String code = link.getShortCode();

        // первый переход — ok
        String url = service.resolveShortLink(code);
        assertEquals("https://example.com", url);

        // второй — ClickLimitExceededException
        assertThrows(ClickLimitExceededException.class,
                () -> service.resolveShortLink(code));
    }

    @Test
    void expiredLinkThrowsLinkExpiredException() throws Exception {
        UserProfile u = createUser(10, 1); // 1 час

        // создаём ссылку
        ShortLink link = service.createShortLink(u.getId(), "https://example.com");

        // имитируем "протухшую" ссылку: двигаем expiresAt назад через reflection-хак
        // (или можно сделать отдельный конструктор ShortLink для тестов)
        // Здесь простой путь: руками кладём устаревшую ссылку в репозиторий
        ShortLink expired = new ShortLink(
                link.getId(),
                link.getShortCode(),
                link.getOriginalUrl(),
                link.getOwnerId(),
                link.getMaxClicks(),
                link.getClickCount(),
                link.getCreatedAt(),
                Instant.now().minus(Duration.ofHours(1)), // уже истекла
                link.isActive()
        );
        linkRepo.save(expired);

        assertThrows(LinkExpiredException.class,
                () -> service.resolveShortLink(link.getShortCode()),
                "При попытке перейти по просроченной ссылке должно кидаться LinkExpiredException");
    }

    @Test
    void userCanOnlySeeOwnLinks() throws Exception {
        UserProfile u1 = createUser(10, 24);
        UserProfile u2 = createUser(10, 24);

        service.createShortLink(u1.getId(), "https://a.com");
        service.createShortLink(u1.getId(), "https://b.com");

        service.createShortLink(u2.getId(), "https://c.com");

        List<ShortLink> linksU1 = service.getUserLinks(u1.getId());
        List<ShortLink> linksU2 = service.getUserLinks(u2.getId());

        assertEquals(2, linksU1.size());
        assertEquals(1, linksU2.size());
    }

    @Test
    void userCannotDeleteOtherUsersLink() throws Exception {
        UserProfile owner = createUser(10, 24);
        UserProfile stranger = createUser(10, 24);

        ShortLink link = service.createShortLink(owner.getId(), "https://secret.com");

        // владелец может удалить
        assertDoesNotThrow(() -> service.deleteUserLink(owner.getId(), link.getShortCode()));

        // возвращаем ссылку, чтобы проверить чужого
        ShortLink link2 = service.createShortLink(owner.getId(), "https://secret2.com");

        assertThrows(AccessDeniedException.class,
                () -> service.deleteUserLink(stranger.getId(), link2.getShortCode()));
    }

    @Test
    void invalidUrlThrowsIllegalArgumentException() {
        UserProfile u = createUser(10, 24);

        assertThrows(IllegalArgumentException.class,
                () -> service.createShortLink(u.getId(), "example.com"),
                "URL без http/https должен быть отклонён");

        assertThrows(IllegalArgumentException.class,
                () -> service.createShortLink(u.getId(), "https://exa mple.com"),
                "URL с некорректным синтаксисом должен быть отклонён");
    }
}
