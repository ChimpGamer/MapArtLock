package nl.chimpgamer.mapartlock.lock;

import java.time.Instant;
import java.util.UUID;

/**
 * The lock carried by one map item: who owns it and since when.
 *
 * <p>There is no separate "locked" flag. The presence of the owner is the lock, which is why
 * nothing on the item has to encode a boolean.
 */
public record MapLock(UUID owner, Instant lockedAt) {
    public MapLock {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        if (lockedAt == null) {
            throw new IllegalArgumentException("lockedAt must not be null");
        }
    }

    public static MapLock now(UUID owner) {
        return new MapLock(owner, Instant.now());
    }

    public boolean isOwnedBy(UUID candidate) {
        return owner.equals(candidate);
    }
}
