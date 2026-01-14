package repository;

import exception.DataAccessException;
import model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<UserProfile> findById(UUID id) throws DataAccessException;

    void save(UserProfile user) throws DataAccessException;
}
