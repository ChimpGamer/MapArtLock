package nl.chimpgamer.mapartlock.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import nl.chimpgamer.mapartlock.config.Messages;
import nl.chimpgamer.mapartlock.config.Settings;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The lore and glint on a locked map. Cosmetic only — the registry is the truth.
 *
 * <p>What this plugin added is remembered verbatim on the item, so removing it never touches
 * lore another plugin owns, and changing messages.yml cannot orphan old lines.
 */
public final class MapDecorator {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String SEPARATOR = "\n";

    private final Settings settings;
    private final Messages messages;
    private final NamespacedKey loreKey;
    private final NamespacedKey glintKey;

    public MapDecorator(Plugin plugin, Settings settings, Messages messages) {
        this.settings = settings;
        this.messages = messages;
        this.loreKey = new NamespacedKey(plugin, "applied_lore");
        this.glintKey = new NamespacedKey(plugin, "applied_glint");
    }

    public void markLocked(ItemStack itemStack, String ownerName) {
        clear(itemStack);

        List<Component> ours = lockedLore(ownerName);
        itemStack.editMeta(meta -> {
            List<Component> lore = new ArrayList<>(Objects.requireNonNullElseGet(meta.lore(), List::<Component>of));
            lore.addAll(ours);
            meta.lore(lore);
            if (settings.glint()) {
                meta.setEnchantmentGlintOverride(true);
            }
        });

        itemStack.editPersistentDataContainer(container -> {
            container.set(loreKey, PersistentDataType.STRING, serialize(ours));
            if (settings.glint()) {
                container.set(glintKey, PersistentDataType.BOOLEAN, true);
            }
        });
    }

    /** Removes only what this plugin added. */
    public void clear(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        String applied = meta.getPersistentDataContainer().get(loreKey, PersistentDataType.STRING);
        boolean glintApplied = Boolean.TRUE.equals(
                meta.getPersistentDataContainer().get(glintKey, PersistentDataType.BOOLEAN));

        if (applied == null && !glintApplied) {
            return;
        }

        List<String> ours = applied == null || applied.isEmpty()
                ? List.of()
                : List.of(applied.split(SEPARATOR, -1));

        itemStack.editMeta(edited -> {
            if (!ours.isEmpty()) {
                List<Component> lore = new ArrayList<>(Objects.requireNonNullElseGet(edited.lore(), List::<Component>of));
                lore.removeIf(line -> ours.contains(MINI_MESSAGE.serialize(line)));
                edited.lore(lore.isEmpty() ? null : lore);
            }
            if (glintApplied) {
                // Only reset an override this plugin set, so an enchantment's glint survives.
                edited.setEnchantmentGlintOverride(null);
            }
        });

        itemStack.editPersistentDataContainer(container -> {
            container.remove(loreKey);
            container.remove(glintKey);
        });
    }

    public boolean isDecorated(ItemStack itemStack) {
        return itemStack != null
                && itemStack.hasItemMeta()
                && itemStack.getItemMeta().getPersistentDataContainer().has(loreKey, PersistentDataType.STRING);
    }

    private List<Component> lockedLore(String ownerName) {
        List<Component> lines = new ArrayList<>();
        lines.add(plain(messages.render("lore.header")));
        lines.add(plain(messages.render("lore.status")));
        if (settings.showOwnerInLore()) {
            lines.add(plain(messages.render("lore.owner", Placeholder.unparsed("owner", ownerName))));
        }
        return List.copyOf(lines);
    }

    private Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private String serialize(List<Component> lines) {
        return lines.stream()
                .map(MINI_MESSAGE::serialize)
                .reduce((first, second) -> first + SEPARATOR + second)
                .orElse("");
    }
}
