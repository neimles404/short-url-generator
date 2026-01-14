package model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ShortLink {
    private final String id;
    private final String shortCode;
    private final String originalUrl;
    private final UUID ownerId;

    private final int maxClicks;
    private int clickCount;

    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean active;

    public ShortLink(String id,
                     String shortCode,
                     String originalUrl,
                     UUID ownerId,
                     int maxClicks,
                     int clickCount,
                     Instant createdAt,
                     Instant expiresAt,
                     boolean active) {
        this.id = Objects.requireNonNull(id);
        this.shortCode = Objects.requireNonNull(shortCode);
        this.originalUrl = Objects.requireNonNull(originalUrl);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.maxClicks = maxClicks;
        this.clickCount = clickCount;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public int getMaxClicks() {
        return maxClicks;
    }

    public int getClickCount() {
        return clickCount;
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isClickLimitExceeded() {
        return clickCount >= maxClicks;
    }

    public void incrementClick() {
        this.clickCount++;
    }
}
