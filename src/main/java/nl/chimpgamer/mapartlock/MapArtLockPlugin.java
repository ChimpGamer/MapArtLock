package nl.chimpgamer.mapartlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import nl.chimpgamer.mapartlock.config.Messages;
import nl.chimpgamer.mapartlock.config.Settings;
import nl.chimpgamer.mapartlock.item.MapDecorator;
import nl.chimpgamer.mapartlock.listener.InventoryProtectionListener;
import nl.chimpgamer.mapartlock.listener.MenuListener;
import nl.chimpgamer.mapartlock.lock.MapLockService;
import nl.chimpgamer.mapartlock.menu.LockMenu;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Locks live on the map items themselves, so there is nothing to load on startup, nothing to
 * flush on shutdown and no state held between the two.
 */
public final class MapArtLockPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();

        Settings settings = new Settings(this);
        Messages messages = new Messages(this);

        MapDecorator decorator = new MapDecorator(this, settings, messages);
        MapLockService service = new MapLockService(this, settings, decorator);
        LockMenu menu = new LockMenu(service, messages);

        registerCommand("mapartlock", plainText(messages.render("command_description")), List.of(),
                new MapArtLockCommand(this, settings, messages, menu));

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new MenuListener(this, settings, service, menu, messages), this);
        plugins.registerEvents(new InventoryProtectionListener(settings, service, messages), this);
    }

    /** The command description is shown in /help, so it belongs in messages.yml like other player-facing text. */
    private static String plainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
