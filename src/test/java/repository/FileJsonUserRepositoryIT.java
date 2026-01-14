package repository;

import model.UserProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FileJsonUserRepositoryIT {

    @Test
    void saveAndLoadUserProfileFromJsonFile() throws Exception {
        Path tempFile = Files.createTempFile("users-", ".json");
        FileJsonUserRepository repo = new FileJsonUserRepository(tempFile.toString());

        UUID id = UUID.randomUUID();
        UserProfile user = new UserProfile(id, 20, 12);
        repo.save(user);

        FileJsonUserRepository repo2 = new FileJsonUserRepository(tempFile.toString());
        var loaded = repo2.findById(id);

        assertTrue(loaded.isPresent());
        assertEquals(20, loaded.get().getDefaultMaxClicks());
        assertEquals(12, loaded.get().getTtlHours());
    }
}
