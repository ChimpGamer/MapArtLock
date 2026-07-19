package nl.chimpgamer.mapartlock;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import nl.chimpgamer.mapartlock.config.Messages;
import nl.chimpgamer.mapartlock.config.Settings;
import nl.chimpgamer.mapartlock.menu.LockMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * {@code /mapartlock} opens the menu; {@code /mapartlock reload} is the only sub-command.
 *
 * <p>There is deliberately no {@code version} or {@code info}: the server already answers
 * {@code /version MapArtLock}, and the state worth reporting — lock count and which protections
 * are live — is reported by reload, which is the one moment it can change.
 */
public final class MapArtLockCommand implements BasicCommand {
    private static final String RELOAD = "reload";

    private final JavaPlugin plugin;
    private final Settings settings;
    private final Messages messages;
    private final LockMenu menu;

    public MapArtLockCommand(JavaPlugin plugin, Settings settings, Messages messages,
                             LockMenu menu) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.menu = menu;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            openMenu(sender);
            return;
        }

        if (RELOAD.equalsIgnoreCase(args[0])) {
            reload(sender);
            return;
        }

        messages.send(sender, Permissions.canReload(sender) ? "usage_admin" : "usage");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return Permissions.canUse(sender) || Permissions.canReload(sender);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length > 1 || !Permissions.canReload(source.getSender())) {
            return List.of();
        }

        String partial = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        return RELOAD.startsWith(partial) ? List.of(RELOAD) : List.of();
    }

    @Override
    public @Nullable String permission() {
        return null;
    }

    private void openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players_only");
            return;
        }

        if (!Permissions.canUse(player)) {
            messages.send(player, "no_permission");
            return;
        }

        menu.open(player);
    }

    private void reload(CommandSender sender) {
        if (!Permissions.canReload(sender)) {
            messages.send(sender, "no_permission");
            return;
        }

        plugin.reloadConfig();
        messages.reload();

        messages.send(sender, "config_reloaded",
                Placeholder.component("protections", activeProtections()));
    }

    /**
     * Names come from messages.yml rather than the code, so the summary stays in the server's
     * own language without a recompile.
     */
    private Component activeProtections() {
        List<Component> active = new ArrayList<>();
        addIfEnabled(active, settings.protectAnvil(), "protections.anvil");
        addIfEnabled(active, settings.protectCraftingTable(), "protections.crafting_table");
        addIfEnabled(active, settings.protectCartographyTable(), "protections.cartography_table");
        addIfEnabled(active, settings.protectCrafter(), "protections.crafter");
        addIfEnabled(active, settings.protectAgainstHoppers(), "protections.hoppers");

        if (active.isEmpty()) {
            return messages.render("protections.none");
        }
        return Component.join(
                JoinConfiguration.separator(messages.render("protections.separator")), active);
    }

    private void addIfEnabled(List<Component> target, boolean enabled, String key) {
        if (enabled) {
            target.add(messages.render(key));
        }
    }
}
