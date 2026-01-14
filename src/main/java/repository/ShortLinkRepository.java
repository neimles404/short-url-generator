package repository;

import exception.DataAccessException;
import model.ShortLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShortLinkRepository {

    void save(ShortLink link) throws DataAccessException;

    Optional<ShortLink> findByShortCode(String shortCode) throws DataAccessException;

    List<ShortLink> findByOwner(UUID ownerId) throws DataAccessException;

    void deleteById(String id) throws DataAccessException;

    void deleteExpired(Instant now) throws DataAccessException;

    boolean shortCodeExists(String shortCode) throws DataAccessException;
}
