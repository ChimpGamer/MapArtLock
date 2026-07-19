package nl.chimpgamer.mapartlock.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import nl.chimpgamer.mapartlock.config.Messages;
import nl.chimpgamer.mapartlock.lock.MapLock;
import nl.chimpgamer.mapartlock.lock.MapLockService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class LockMenu {
    public static final int SIZE = 27;
    public static final int LOCK_SLOT = 11;
    public static final int MAP_SLOT = 13;
    public static final int UNLOCK_SLOT = 15;

    private final MapLockService service;
    private final Messages messages;

    public LockMenu(MapLockService service, Messages messages) {
        this.service = service;
        this.messages = messages;
    }

    public void open(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!service.isFilledMap(held)) {
            messages.send(player, "must_hold_map");
            return;
        }

        OptionalInt mapId = service.mapId(held);
        if (mapId.isEmpty()) {
            messages.send(player, "map_has_no_data");
            return;
        }

        Holder holder = new Holder(mapId.getAsInt());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, messages.render("menu.title"));
        holder.inventory = inventory;
        populate(inventory, held);
        player.openInventory(inventory);
    }

    private void populate(Inventory inventory, ItemStack map) {
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, Component.empty(), null);
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        boolean locked = service.isLocked(map);

        inventory.setItem(LOCK_SLOT, button(
                locked ? Material.GRAY_DYE : Material.LIME_DYE,
                messages.render("menu.lock.name"),
                messages.render(locked ? "menu.lock.lore_disabled" : "menu.lock.lore")));

        inventory.setItem(MAP_SLOT, describe(map));

        inventory.setItem(UNLOCK_SLOT, button(
                locked ? Material.RED_DYE : Material.GRAY_DYE,
                messages.render("menu.unlock.name"),
                messages.render(locked ? "menu.unlock.lore" : "menu.unlock.lore_disabled")));
    }

    /** The held map itself, with the lock state written into its lore. */
    private ItemStack describe(ItemStack map) {
        ItemStack display = map.clone();
        Optional<MapLock> lock = service.lockOf(map);

        List<Component> lines = new ArrayList<>();
        lines.add(plain(messages.render("menu.info.map_id",
                Placeholder.unparsed("map_id", String.valueOf(service.mapId(map).orElse(-1))))));

        if (lock.isPresent()) {
            lines.add(plain(messages.render("menu.info.status_locked")));
            lines.add(plain(messages.render("menu.info.owner",
                    Placeholder.unparsed("owner", service.ownerName(lock.get().owner())))));
            lines.add(plain(messages.render("menu.info.locked_at",
                    Formatter.date("locked_at", lock.get().lockedAt().atZone(ZoneId.systemDefault())))));
        } else {
            lines.add(plain(messages.render("menu.info.status_unlocked")));
        }

        display.editMeta(meta -> meta.lore(lines));
        return display;
    }

    private ItemStack button(Material material, Component name, Component lore) {
        ItemStack itemStack = new ItemStack(material);
        itemStack.editMeta(meta -> {
            meta.displayName(plain(name));
            if (lore != null) {
                meta.lore(List.of(plain(lore)));
            }
        });
        return itemStack;
    }

    private Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    /** Remembers which map the menu was opened for, so a swapped hand cannot be acted on. */
    public static final class Holder implements InventoryHolder {
        private final int mapId;
        private Inventory inventory;

        private Holder(int mapId) {
            this.mapId = mapId;
        }

        public int mapId() {
            return mapId;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
