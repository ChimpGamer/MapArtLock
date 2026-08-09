package nl.chimpgamer.mapartlock.lock;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapId;
import nl.chimpgamer.mapartlock.Permissions;
import nl.chimpgamer.mapartlock.config.Settings;
import nl.chimpgamer.mapartlock.item.MapLore;
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
 * <p>Only two PDC keys are written: {@code owner} and {@code locked_at}. The presence of the
 * owner is the whole lock — there is no separate "locked" flag. The visible lore is not stored
 * again in the item's data; it already lives in the item meta, so {@link MapLore} re-renders the
 * same lines to remove them rather than keeping a duplicate copy.
 *
 * <p>The lock lives on the item itself, so there is nothing to load, nothing to save and nothing
 * held in memory. Protection is as durable as the item and travels with it into chests and
 * backups. The trade-off is that it follows the item, not the artwork: whoever can run
 * {@code /give filled_map[map_id=...]} gets an unprotected copy — but with those powers they
 * could disable this plugin anyway.
 */
public final class MapLockService {
    private final Settings settings;
    private final MapLore lore;
    private final NamespacedKey ownerKey;
    private final NamespacedKey lockedAtKey;

    public MapLockService(Plugin plugin, Settings settings, MapLore lore) {
        this.settings = settings;
        this.lore = lore;
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
        lore.apply(itemStack, ownerName(lock.owner()));
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

        lore.remove(itemStack, ownerName(existing.get().owner()));
        itemStack.editPersistentDataContainer(container -> {
            container.remove(ownerKey);
            container.remove(lockedAtKey);
        });
        return LockOutcome.UNLOCKED;
    }

    public String ownerName(UUID owner) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
        String name = offlinePlayer.getName();
        return name == null ? owner.toString() : name;
    }
}
