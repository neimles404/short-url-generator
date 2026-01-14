package service;

import exception.*;
import model.ShortLink;
import model.UserProfile;
import repository.ShortLinkRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import config.AppConfig;
import repository.UserRepository;


public class UrlShortenerService {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final int shortCodeLength;
    private final Duration linkTtl;
    private final int minClicksAllowed;
    private final int maxClicksAllowed;

    private final ShortLinkRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final UserRepository userRepository;

    public UrlShortenerService(ShortLinkRepository repository, AppConfig config, UserRepository userRepository) {
        this.repository = repository;
        this.shortCodeLength = config.shortCodeLength();
        this.linkTtl = config.linkTtl();
        this.minClicksAllowed = config.minClicksAllowed();
        this.maxClicksAllowed = config.maxClicksAllowed();
        this.userRepository = userRepository;
    }


    public ShortLink createShortLink(UUID userId, String originalUrl)
            throws DataAccessException {

        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("URL не может быть пустым");
        }

        try {
            URI uri = new URI(originalUrl.trim());
            String scheme = uri.getScheme();

            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("URL должен начинаться с http:// или https://");
            }
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("Некорректный URL: отсутствует домен");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Некорректный формат URL: " + e.getMessage());
        }

        UserProfile user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        int maxClicks = user.getDefaultMaxClicks();
        if (maxClicks < minClicksAllowed || maxClicks > maxClicksAllowed) {
            throw new IllegalStateException("У пользователя заданы некорректные настройки лимита");
        }

        Duration ttl = Duration.ofHours(user.getTtlHours());
        if (ttl.isZero() || ttl.isNegative()) {
            ttl = linkTtl;
        }

        String shortCode = generateUniqueShortCode();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

        ShortLink link = new ShortLink(
                UUID.randomUUID().toString(),
                shortCode,
                originalUrl,
                userId,
                maxClicks,
                0,
                now,
                expiresAt,
                true
        );

        repository.save(link);
        return link;
    }


    public String resolveShortLink(String shortCode)
            throws DataAccessException, LinkNotFoundException,
            LinkExpiredException, ClickLimitExceededException {

        Instant now = Instant.now();

        var opt = repository.findByShortCode(shortCode);
        if (opt.isEmpty()) {
            throw new LinkNotFoundException("Ссылка не найдена");
        }

        ShortLink link = opt.get();

        if (!link.isActive()) {
            throw new ClickLimitExceededException("Ссылка деактивирована");
        }

        if (link.isExpired(now)) {
            repository.deleteById(link.getId());
            throw new LinkExpiredException("Срок жизни ссылки истёк");
        }

        if (link.isClickLimitExceeded()) {
            link.deactivate();
            repository.save(link);
            throw new ClickLimitExceededException("Лимит переходов исчерпан");
        }

        link.incrementClickCount();
        if (link.isClickLimitExceeded()) {
            link.deactivate();
        }
        repository.save(link);

        return link.getOriginalUrl();
    }

    public List<ShortLink> getUserLinks(UUID userId) throws DataAccessException {
        return repository.findByOwner(userId);
    }

    public void deleteUserLink(UUID userId, String shortCode)
            throws DataAccessException, LinkNotFoundException, AccessDeniedException {

        var opt = repository.findByShortCode(shortCode);
        if (opt.isEmpty()) {
            throw new LinkNotFoundException("Ссылка не найдена");
        }
        ShortLink link = opt.get();
        if (!link.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Нельзя удалить ссылку другого пользователя");
        }
        repository.deleteById(link.getId());
    }

    public void cleanupExpiredLinks() throws DataAccessException {
        repository.deleteExpired(Instant.now());
    }

    private String generateUniqueShortCode() throws DataAccessException {
        while (true) {
            String code = randomCode();
            if (!repository.shortCodeExists(code)) {
                return code;
            }
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(shortCodeLength);
        for (int i = 0; i < shortCodeLength; i++) {
            int idx = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        return sb.toString();
    }
}
