package nl.chimpgamer.mapartlock.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Loads messages.yml and renders it as MiniMessage.
 *
 * <p>The prefix is exposed as a {@code <prefix>} tag rather than glued on automatically, so
 * every message decides for itself whether it wants one — and it stays defined in a single
 * place, so changing it is a one-line edit.
 */
public final class Messages {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String PREFIX_KEY = "prefix";

    private final Plugin plugin;
    private final File file;
    private FileConfiguration bundled;
    private FileConfiguration current;

    public Messages(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        reload();
    }

    public void reload() {
        bundled = loadBundled();
        current = YamlConfiguration.loadConfiguration(file);
        current.setDefaults(bundled);
        writeMissingDefaults();
    }

    public void send(CommandSender recipient, String key, TagResolver... resolvers) {
        recipient.sendMessage(render(key, resolvers));
    }

    /**
     * Renders a message. {@code <prefix>} resolves to the prefix key; leave it out and the
     * message has none. That is why item lore and menu titles need no separate method.
     */
    public Component render(String key, TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(raw(key), prefixTag(), TagResolver.resolver(resolvers));
    }

    private TagResolver prefixTag() {
        return Placeholder.parsed(PREFIX_KEY, current.getString(PREFIX_KEY, ""));
    }

    private String raw(String key) {
        if (PREFIX_KEY.equals(key)) {
            return current.getString(key, "");
        }
        return current.getString(key, "<red>Missing Messages: " + key);
    }

    private FileConfiguration loadBundled() {
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not load the bundled messages.yml", exception);
            return new YamlConfiguration();
        }
    }

    /** Adds keys the operator has not seen yet, without touching what they already changed. */
    private void writeMissingDefaults() {
        boolean changed = false;
        for (String key : bundled.getKeys(true)) {
            if (!bundled.isConfigurationSection(key) && !current.contains(key, true)) {
                current.set(key, bundled.get(key));
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        try {
            current.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not write missing defaults to messages.yml", exception);
        }
    }
}
