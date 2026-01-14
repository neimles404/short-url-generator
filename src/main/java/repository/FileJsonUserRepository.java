package repository;

import exception.DataAccessException;
import model.UserProfile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class FileJsonUserRepository implements UserRepository {

    private final Path filePath;
    private final Map<UUID, UserProfile> storage = new HashMap<>();

    public FileJsonUserRepository(String fileName) throws DataAccessException {
        this.filePath = Paths.get(fileName);
        loadFromFile();
    }

    @Override
    public synchronized Optional<UserProfile> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public synchronized void save(UserProfile user) throws DataAccessException {
        storage.put(user.getId(), user);
        flushToFile();
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
                UserProfile user = jsonToUser(obj);
                storage.put(user.getId(), user);
            }
        } catch (IOException e) {
            throw new DataAccessException("Ошибка чтения файла пользователей", e);
        } catch (Exception e) {
            throw new DataAccessException("Некорректный формат JSON users.json", e);
        }
    }

    private void flushToFile() throws DataAccessException {
        try {
            JSONArray array = new JSONArray();
            for (UserProfile u : storage.values()) {
                array.put(userToJson(u));
            }
            Files.writeString(filePath, array.toString(2),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new DataAccessException("Ошибка записи файла пользователей", e);
        }
    }

    private JSONObject userToJson(UserProfile u) {
        JSONObject obj = new JSONObject();
        obj.put("id", u.getId().toString());
        obj.put("defaultMaxClicks", u.getDefaultMaxClicks());
        obj.put("ttlHours", u.getTtlHours());
        return obj;
    }

    private UserProfile jsonToUser(JSONObject obj) {
        return new UserProfile(
                UUID.fromString(obj.getString("id")),
                obj.getInt("defaultMaxClicks"),
                obj.getLong("ttlHours")
        );
    }
}
