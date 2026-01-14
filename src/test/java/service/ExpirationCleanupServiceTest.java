package service;

import config.AppConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class ExpirationCleanupServiceTest {

    static class FakeUrlService extends UrlShortenerService {
        private int cleanupCalls = 0;

        public FakeUrlService() {
            super(
                new service.InMemoryShortLinkRepository(),
                new AppConfig(
                    Duration.ofHours(24),
                    6,
                    1,
                    1000,
                    10,
                    "src/db/test-links.json",
                    "src/db/test-users.json",
                    "clck.test"
                ),
                new service.InMemoryUserRepository()
            );

        }

        @Override
        public void cleanupExpiredLinks() {
            cleanupCalls++;
        }

        public int getCleanupCalls() {
            return cleanupCalls;
        }
    }

    @Test
    void cleanupServiceCallsCleanupPeriodically() throws InterruptedException {
        FakeUrlService fakeService = new FakeUrlService();
        // интервал 100 мс для теста
        ExpirationCleanupService cleanupService = new ExpirationCleanupService(fakeService, 100);

        cleanupService.start();
        Thread.sleep(350); // подождём несколько интервалов
        cleanupService.stop();

        assertTrue(fakeService.getCleanupCalls() >= 2,
                "Ожидалось, что cleanupExpiredLinks будет вызван несколько раз");
    }
}
