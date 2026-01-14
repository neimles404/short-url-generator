package repository;

import org.json.JSONArray;
import org.json.JSONObject;
import exception.DataAccessException;
import model.ShortLink;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class FileJsonShortLinkRepository implements ShortLinkRepository {

    private final Path filePath;
    private final Map<String, ShortLink> storage = new HashMap<>();

    public FileJsonShortLinkRepository(String fileName) throws DataAccessException {
        this.filePath = Paths.get(fileName);
        loadFromFile();
    }

    @Override
    public synchronized void save(ShortLink link) throws DataAccessException {
        storage.put(link.getId(), link);
        flushToFile();
    }

    @Override
    public synchronized Optional<ShortLink> findByShortCode(String shortCode) {
        return storage.values().stream()
                .filter(l -> l.getShortCode().equals(shortCode))
                .findFirst();
    }

    @Override
    public synchronized List<ShortLink> findByOwner(UUID ownerId) {
        return storage.values().stream()
                .filter(l -> l.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void deleteById(String id) throws DataAccessException {
        storage.remove(id);
        flushToFile();
    }

    @Override
    public synchronized void deleteExpired(Instant now) throws DataAccessException {
        List<String> toRemove = storage.values().stream()
                .filter(l -> l.isExpired(now))
                .map(ShortLink::getId)
                .toList();
        toRemove.forEach(storage::remove);
        flushToFile();
    }

    @Override
    public synchronized boolean shortCodeExists(String shortCode) {
        return storage.values().stream()
                .anyMatch(l -> l.getShortCode().equals(shortCode));
    }

    private void loadFromFile() throws DataAccessException {
        try {
            if (Files.notExists(filePath)) {
                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                Files.createFile(filePath);
                Files.writeString(filePath, "[]");
            }
            String content = Files.readString(filePath).trim();
            if (content.isEmpty()) {
                content = "[]";
            }
            JSONArray array = new JSONArray(content);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ShortLink link = jsonToShortLink(obj);
                storage.put(link.getId(), link);
            }
        } catch (IOException e) {
            throw new DataAccessException("Ошибка чтения файла базы данных", e);
        } catch (Exception e) {
            throw new DataAccessException("Некорректный формат JSON базы данных", e);
        }
    }

    private void flushToFile() throws DataAccessException {
        try {
            JSONArray array = new JSONArray();
            for (ShortLink link : storage.values()) {
                array.put(shortLinkToJson(link));
            }
            Files.writeString(filePath, array.toString(2),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new DataAccessException("Ошибка записи файла базы данных", e);
        }
    }

    private JSONObject shortLinkToJson(ShortLink link) {
        JSONObject obj = new JSONObject();
        obj.put("id", link.getId());
        obj.put("shortCode", link.getShortCode());
        obj.put("originalUrl", link.getOriginalUrl());
        obj.put("ownerId", link.getOwnerId().toString());
        obj.put("maxClicks", link.getMaxClicks());
        obj.put("clickCount", link.getClickCount());
        obj.put("createdAt", link.getCreatedAt().toString());
        obj.put("expiresAt", link.getExpiresAt().toString());
        obj.put("active", link.isActive());
        return obj;
    }

    private ShortLink jsonToShortLink(JSONObject obj) {
        return new ShortLink(
                obj.getString("id"),
                obj.getString("shortCode"),
                obj.getString("originalUrl"),
                UUID.fromString(obj.getString("ownerId")),
                obj.getInt("maxClicks"),
                obj.getInt("clickCount"),
                Instant.parse(obj.getString("createdAt")),
                Instant.parse(obj.getString("expiresAt")),
                obj.getBoolean("active")
        );
    }
}
