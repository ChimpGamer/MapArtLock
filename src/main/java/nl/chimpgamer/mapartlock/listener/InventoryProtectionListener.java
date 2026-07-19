package nl.chimpgamer.mapartlock.listener;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import nl.chimpgamer.mapartlock.config.Messages;
import nl.chimpgamer.mapartlock.config.Settings;
import nl.chimpgamer.mapartlock.lock.MapLockService;
import org.bukkit.block.Crafter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Keeps locked maps out of everything that could duplicate or alter them.
 *
 * <p>Two layers: refusing the item on the way in, which gives the player a reason, and removing
 * the result, which is the backstop that also covers the 2x2 grid in the player's own inventory.
 */
public final class InventoryProtectionListener implements Listener {
    private final Settings settings;
    private final MapLockService service;
    private final Messages messages;

    public InventoryProtectionListener(Settings settings, MapLockService service, Messages messages) {
        this.settings = settings;
        this.service = service;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isProtected(top) || !(event.getWhoClicked() instanceof Player player) || service.hasBypass(player)) {
            return;
        }

        if (movesLockedMapIn(event, top, player)) {
            event.setCancelled(true);
            messages.send(player, "protected_inventory_blocked");
        }
    }

    private boolean movesLockedMapIn(InventoryClickEvent event, Inventory top, Player player) {
        int rawSlot = event.getRawSlot();
        boolean clickedTop = rawSlot >= 0 && rawSlot < top.getSize();

        // Shift-clicking from the player's own inventory lands in the top one.
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && !clickedTop) {
            return blocked(event.getCurrentItem(), player);
        }

        if (!clickedTop) {
            return false;
        }

        return blocked(event.getCurrentItem(), player)
                || blocked(event.getCursor(), player)
                || event.getClick() == ClickType.NUMBER_KEY && blockedHotbar(event.getHotbarButton(), player)
                || event.getClick() == ClickType.SWAP_OFFHAND && blocked(player.getInventory().getItemInOffHand(), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isProtected(top) || !(event.getWhoClicked() instanceof Player player) || service.hasBypass(player)) {
            return;
        }

        boolean draggedIn = event.getNewItems().entrySet().stream()
                .anyMatch(entry -> entry.getKey() < top.getSize() && blocked(entry.getValue(), player));
        if (draggedIn) {
            event.setCancelled(true);
            messages.send(player, "protected_inventory_blocked");
        }
    }

    /** Hoppers have no player behind them, so nobody could hold the bypass permission. */
    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (settings.protectAgainstHoppers()
                && isProtected(event.getDestination())
                && service.isLocked(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (settings.protectCraftingTable() && containsBlocked(event, event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        Inventory inventory = event.getInventory();
        if (settings.protectAnvil() && containsBlocked(event, inventory.getItem(0), inventory.getItem(1))) {
            event.setResult(null);
        }
    }

    /** Paper's variant, which also fires for the cartography table. */
    @EventHandler
    public void onPrepareResult(PrepareResultEvent event) {
        if (settings.protectCartographyTable()
                && event.getInventory().getType() == org.bukkit.event.inventory.InventoryType.CARTOGRAPHY
                && containsBlocked(event, event.getInventory().getContents())) {
            event.setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!settings.protectCrafter() || !(event.getBlock().getState() instanceof Crafter crafter)) {
            return;
        }

        for (ItemStack itemStack : crafter.getInventory().getContents()) {
            if (service.isLocked(itemStack)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean containsBlocked(InventoryEvent event, ItemStack... contents) {
        if (contents == null) {
            return false;
        }

        for (ItemStack itemStack : contents) {
            if (blockedForAnyViewer(event, itemStack)) {
                return true;
            }
        }
        return false;
    }

    /** The result is shared by everyone looking, so one disallowed viewer is enough to drop it. */
    private boolean blockedForAnyViewer(InventoryEvent event, ItemStack itemStack) {
        if (!service.isLocked(itemStack)) {
            return false;
        }

        for (HumanEntity viewer : event.getInventory().getViewers()) {
            if (!(viewer instanceof Player player) || !service.mayUse(itemStack, player)) {
                return true;
            }
        }
        return false;
    }

    private boolean blockedHotbar(int hotbarSlot, Player player) {
        return hotbarSlot >= 0 && blocked(player.getInventory().getItem(hotbarSlot), player);
    }

    private boolean blocked(ItemStack itemStack, Player player) {
        return itemStack != null && !service.mayUse(itemStack, player);
    }

    private boolean isProtected(Inventory inventory) {
        return switch (inventory.getType()) {
            case ANVIL -> settings.protectAnvil();
            case CARTOGRAPHY -> settings.protectCartographyTable();
            case CRAFTER -> settings.protectCrafter();
            case WORKBENCH -> settings.protectCraftingTable();
            // CRAFTING is the 2x2 grid in the player's own inventory. Treating it as protected
            // would block ordinary inventory handling, so cloning there is stopped at the result.
            default -> false;
        };
    }
}
