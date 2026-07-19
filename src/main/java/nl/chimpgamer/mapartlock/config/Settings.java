package nl.chimpgamer.mapartlock.config;

import org.bukkit.plugin.Plugin;

/**
 * Reads config.yml on demand, the same way {@link Messages} reads its file.
 *
 * <p>Nothing is copied into fields: Bukkit already holds the parsed tree, so a second copy would
 * only add a way for the two to disagree. It also means {@code reloadConfig()} is enough to make
 * a change take effect — there is no cache to refresh.
 */
public final class Settings {
    private final Plugin plugin;

    public Settings(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean openMenuOnSneakRightClick() {
        return flag("menu.open-on-sneak-right-click");
    }

    public boolean glint() {
        return flag("appearance.glint");
    }

    public boolean showOwnerInLore() {
        return flag("appearance.show-owner-in-lore");
    }

    public boolean protectAnvil() {
        return flag("protection.anvil");
    }

    public boolean protectCraftingTable() {
        return flag("protection.crafting-table");
    }

    public boolean protectCartographyTable() {
        return flag("protection.cartography-table");
    }

    public boolean protectCrafter() {
        return flag("protection.crafter");
    }

    public boolean protectAgainstHoppers() {
        return flag("protection.hoppers");
    }

    /** Whether {@code mapartlock.bypass} does anything at all. */
    public boolean bypassEnabled() {
        return flag("admin.bypass-enabled");
    }

    public boolean logActions() {
        return flag("admin.log-actions");
    }

    /** Everything defaults to on, so a missing key protects rather than exposes. */
    private boolean flag(String path) {
        return plugin.getConfig().getBoolean(path, true);
    }
}
