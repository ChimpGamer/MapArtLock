package nl.chimpgamer.mapartlock.listener;

import nl.chimpgamer.mapartlock.Permissions;
import nl.chimpgamer.mapartlock.config.Messages;
import nl.chimpgamer.mapartlock.config.Settings;
import nl.chimpgamer.mapartlock.lock.LockOutcome;
import nl.chimpgamer.mapartlock.lock.MapLockService;
import nl.chimpgamer.mapartlock.menu.LockMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.OptionalInt;

/** Opening the menu, and everything that happens inside it. */
public final class MenuListener implements Listener {
    private final Plugin plugin;
    private final Settings settings;
    private final MapLockService service;
    private final LockMenu menu;
    private final Messages messages;

    public MenuListener(Plugin plugin, Settings settings, MapLockService service, LockMenu menu, Messages messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.service = service;
        this.menu = menu;
        this.messages = messages;
    }

    /**
     * Sneak is required so a plain right click still puts a map into an item frame.
     *
     * <p>Runs without {@code ignoreCancelled} and at {@code LOWEST} on purpose: Paper fires this
     * event already cancelled "if the vanilla behaviour is to do nothing (e.g. interacting with
     * air)", and a filled map has no right-click action. Skipping cancelled events would drop
     * every click aimed at the sky.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!settings.openMenuOnSneakRightClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!event.getPlayer().isSneaking() || !service.isFilledMap(event.getItem())) {
            return;
        }

        if (!Permissions.canUse(event.getPlayer())) {
            messages.send(event.getPlayer(), "no_permission");
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        menu.open(event.getPlayer());
    }

    /** Every action closes the menu, which is what keeps players from flooding chat and the log. */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof LockMenu.Holder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot != LockMenu.LOCK_SLOT && slot != LockMenu.UNLOCK_SLOT) {
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!holdsMenuMap(holder, held)) {
            player.closeInventory();
            messages.send(player, "must_hold_map");
            return;
        }

        LockOutcome outcome = slot == LockMenu.LOCK_SLOT
                ? service.lock(held, player)
                : service.unlock(held, player);

        player.closeInventory();
        messages.send(player, outcome.messageKey());
        log(player, holder.mapId(), outcome);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LockMenu.Holder) {
            event.setCancelled(true);
        }
    }

    /** Guards against the player swapping their held item while the menu is open. */
    private boolean holdsMenuMap(LockMenu.Holder holder, ItemStack held) {
        OptionalInt mapId = service.mapId(held);
        return mapId.isPresent() && mapId.getAsInt() == holder.mapId();
    }

    private void log(Player player, int mapId, LockOutcome outcome) {
        if (!settings.logActions() || !outcome.changedState()) {
            return;
        }

        String action = outcome == LockOutcome.LOCKED ? "locked" : "unlocked";
        plugin.getLogger().info(() -> "%s %s map art #%d".formatted(player.getName(), action, mapId));
    }
}
