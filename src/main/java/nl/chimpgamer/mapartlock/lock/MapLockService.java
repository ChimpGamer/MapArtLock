package nl.chimpgamer.mapartlock.lock;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import nl.chimpgamer.mapartlock.Permissions;
import nl.chimpgamer.mapartlock.config.Settings;
import nl.chimpgamer.mapartlock.item.MapDecorator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Every rule about locking, and the only place that touches the item's data.
 *
 * <p>The lock lives on the item itself, which means there is nothing to load, nothing to save
 * and nothing held in memory. Protection is exactly as durable as the item: it travels along
 * into chests, shulkers and backups on its own.
 *
 * <p>The trade-off is that protection follows the item rather than the artwork. Somebody who
 * can run {@code /give filled_map[map_id=...]} gets an unprotected copy — but that requires
 * powers with which they could also disable this plugin outright.
 */
public final class MapLockService {
    private final Settings settings;
    private final MapDecorator decorator;
    private final NamespacedKey ownerKey;
    private final NamespacedKey lockedAtKey;

    public MapLockService(Plugin plugin, Settings settings, MapDecorator decorator) {
        this.settings = settings;
        this.decorator = decorator;
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.lockedAtKey = new NamespacedKey(plugin, "locked_at");
    }

    public boolean isFilledMap(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() == Material.FILLED_MAP;
    }

    /** The vanilla map id. Only used for display and logging; it is not what identifies a lock. */
    public OptionalInt mapId(ItemStack itemStack) {
        if (!isFilledMap(itemStack)) {
            return OptionalInt.empty();
        }

        MapId mapId = itemStack.getData(DataComponentTypes.MAP_ID);
        return mapId == null ? OptionalInt.empty() : OptionalInt.of(mapId.id());
    }

    public Optional<MapLock> lockOf(ItemStack itemStack) {
        if (!isFilledMap(itemStack) || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }

        PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();
        Long lockedAt = container.get(lockedAtKey, PersistentDataType.LONG);
        if (lockedAt == null) {
            return Optional.empty();
        }

        try {
            UUID owner = container.get(ownerKey, UuidType.INSTANCE);
            return owner == null ? Optional.empty() : Optional.of(new MapLock(owner, Instant.ofEpochSecond(lockedAt)));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    public boolean isLocked(ItemStack itemStack) {
        return lockOf(itemStack).isPresent();
    }

    /** Whether this player may put the item in an anvil, crafting grid, item frame and so on. */
    public boolean mayUse(ItemStack itemStack, Player player) {
        Optional<MapLock> lock = lockOf(itemStack);
        return lock.isEmpty() || lock.get().isOwnedBy(player.getUniqueId()) || hasBypass(player);
    }

    public boolean hasBypass(Player player) {
        return settings.bypassEnabled() && Permissions.canBypass(player);
    }

    public LockOutcome lock(ItemStack itemStack, Player player) {
        if (!isFilledMap(itemStack)) {
            return LockOutcome.NOT_A_MAP;
        }
        if (mapId(itemStack).isEmpty()) {
            return LockOutcome.NO_MAP_DATA;
        }
        if (isLocked(itemStack)) {
            return LockOutcome.ALREADY_LOCKED;
        }

        MapLock lock = MapLock.now(player.getUniqueId());
        itemStack.editPersistentDataContainer(container -> {
            container.set(ownerKey, UuidType.INSTANCE, lock.owner());
            container.set(lockedAtKey, PersistentDataType.LONG, lock.lockedAt().getEpochSecond());
        });
        decorator.markLocked(itemStack, ownerName(lock.owner()));
        return LockOutcome.LOCKED;
    }

    public LockOutcome unlock(ItemStack itemStack, Player player) {
        if (!isFilledMap(itemStack)) {
            return LockOutcome.NOT_A_MAP;
        }

        Optional<MapLock> existing = lockOf(itemStack);
        if (existing.isEmpty()) {
            return LockOutcome.ALREADY_UNLOCKED;
        }
        if (!existing.get().isOwnedBy(player.getUniqueId()) && !Permissions.canManageOthers(player)) {
            return LockOutcome.NOT_OWNER;
        }

        itemStack.editPersistentDataContainer(container -> {
            container.remove(ownerKey);
            container.remove(lockedAtKey);
        });
        decorator.clear(itemStack);
        return LockOutcome.UNLOCKED;
    }

    public String ownerName(UUID owner) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
        String name = offlinePlayer.getName();
        return name == null ? owner.toString() : name;
    }
}
