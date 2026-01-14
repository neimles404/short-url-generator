package config;

import exception.ConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public record AppConfig(Duration linkTtl, int shortCodeLength, int minClicksAllowed, int maxClicksAllowed,
                        int defaultMaxClicks, String dbFilePath, String usersDbFilePath, String baseShortUrl) {

    public AppConfig {

        if (linkTtl == null || linkTtl.isZero() || linkTtl.isNegative()) {
            throw new IllegalArgumentException("TTL должен быть больше 0");
        }
        if (shortCodeLength <= 0) {
            throw new IllegalArgumentException("Длина короткого кода должна быть > 0");
        }
        if (minClicksAllowed <= 0 || maxClicksAllowed <= 0 || minClicksAllowed > maxClicksAllowed) {
            throw new IllegalArgumentException("Некорректный диапазон лимита переходов");
        }
        if (defaultMaxClicks < minClicksAllowed || defaultMaxClicks > maxClicksAllowed) {
            throw new IllegalArgumentException("Лимит по умолчанию должен быть в диапазоне min–max");
        }
        if (dbFilePath == null || dbFilePath.isBlank()) {
            throw new IllegalArgumentException("Путь к файлу ссылок не может быть пустым");
        }
        if (usersDbFilePath == null || usersDbFilePath.isBlank()) {
            throw new IllegalArgumentException("Путь к файлу пользователей не может быть пустым");
        }
        if (baseShortUrl == null || baseShortUrl.isBlank()) {
            throw new IllegalArgumentException("Базовый URL не может быть пустым");
        }

    }


    public static AppConfig loadDefault() throws ConfigException {
        return load("app.properties");
    }

    public static AppConfig load(String resourceName) throws ConfigException {
        Properties props = new Properties();

        try (InputStream is = AppConfig.class.getClassLoader()
            .getResourceAsStream(resourceName)) {

            if (is == null) {
                throw new ConfigException("Не найден файл конфигурации: " + resourceName);
            }
            props.load(is);
        } catch (IOException e) {
            throw new ConfigException("Ошибка чтения файла конфигурации: " + e.getMessage(), e);
        }

        try {
            long ttlHours = Long.parseLong(props.getProperty("app.ttl.hours"));
            int shortLen = Integer.parseInt(props.getProperty("app.shortCode.length"));
            int minClicks = Integer.parseInt(props.getProperty("app.clicks.min"));
            int maxClicks = Integer.parseInt(props.getProperty("app.clicks.max"));
            int defaultMaxClicks = Integer.parseInt(props.getProperty("app.clicks.default"));
            String dbFile = props.getProperty("app.db.file");
            String baseUrl = props.getProperty("app.base.url");
            String usersDbFilePath = props.getProperty("app.users.db.file");

            return new AppConfig(
                Duration.ofHours(ttlHours),
                shortLen,
                minClicks,
                maxClicks,
                defaultMaxClicks,
                dbFile,
                usersDbFilePath,
                baseUrl
            );
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Некорректные значения в конфигурации: " + e.getMessage(), e);
        }
    }
}

// 310f0958-81b6-4492-aa66-4c54b180a89d
