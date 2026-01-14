package service;

import exception.DataAccessException;

public class ExpirationCleanupService implements Runnable {

    private final UrlShortenerService urlShortenerService;
    private final long intervalMillis;
    private volatile boolean running = false;
    private Thread workerThread;

    // Раз в час: 60 * 60 * 1000 миллисекунд
    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;

    public ExpirationCleanupService(UrlShortenerService urlShortenerService) {
        this(urlShortenerService, ONE_HOUR_MILLIS);
    }

    public ExpirationCleanupService(UrlShortenerService urlShortenerService, long intervalMillis) {
        this.urlShortenerService = urlShortenerService;
        this.intervalMillis = intervalMillis;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        workerThread = new Thread(this, "expiration-cleanup-thread");
        workerThread.setDaemon(true); // поток-демон, не блокирует завершение JVM
        workerThread.start();
    }

    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                urlShortenerService.cleanupExpiredLinks();
            } catch (DataAccessException e) {
                System.out.println("⚠️ Ошибка фоновой очистки просроченных ссылок: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("⚠️ Неожиданная ошибка в потоке очистки: " + e.getMessage());
            }
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
