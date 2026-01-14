package model;

import java.util.UUID;

public class UserProfile {

    private final UUID id;
    private int defaultMaxClicks;
    private long ttlHours;

    public UserProfile(UUID id, int defaultMaxClicks, long ttlHours) {
        this.id = id;
        this.defaultMaxClicks = defaultMaxClicks;
        this.ttlHours = ttlHours;
    }

    public UUID getId() {
        return id;
    }

    public int getDefaultMaxClicks() {
        return defaultMaxClicks;
    }

    public void setDefaultMaxClicks(int defaultMaxClicks) {
        this.defaultMaxClicks = defaultMaxClicks;
    }

    public long getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(long ttlHours) {
        this.ttlHours = ttlHours;
    }
}
