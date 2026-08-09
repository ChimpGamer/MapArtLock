package nl.chimpgamer.mapartlock.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import nl.chimpgamer.mapartlock.config.Messages;
import nl.chimpgamer.mapartlock.config.Settings;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The visible lore on a protected map. It lives only in the item meta — no copy is kept in the
 * item's data, because the lore is already there to read.
 *
 * <p>Removal re-renders the same lines and drops exactly those, so other plugins' lore is left
 * alone. The one thing it does not survive is editing these texts in messages.yml while
 * protected maps are already in circulation; those keep their old lines until re-protected.
 */
public final class MapLore {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Settings settings;
    private final Messages messages;

    public MapLore(Settings settings, Messages messages) {
        this.settings = settings;
        this.messages = messages;
    }

    public void apply(ItemStack itemStack, String ownerName) {
        List<Component> ours = lines(ownerName);
        itemStack.editMeta(meta -> {
            List<Component> lore = new ArrayList<>(Objects.requireNonNullElseGet(meta.lore(), List::<Component>of));
            lore.addAll(ours);
            meta.lore(lore);
        });
    }

    public void remove(ItemStack itemStack, String ownerName) {
        Set<String> ours = lines(ownerName).stream()
                .map(MINI_MESSAGE::serialize)
                .collect(Collectors.toSet());

        itemStack.editMeta(meta -> {
            List<Component> lore = meta.lore();
            if (lore == null) {
                return;
            }
            List<Component> kept = new ArrayList<>(lore);
            kept.removeIf(line -> ours.contains(MINI_MESSAGE.serialize(line)));
            meta.lore(kept.isEmpty() ? null : kept);
        });
    }

    private List<Component> lines(String ownerName) {
        List<Component> lines = new ArrayList<>();
        lines.add(nonItalic(messages.render("lore.header")));
        lines.add(nonItalic(messages.render("lore.status")));
        if (settings.showOwnerInLore()) {
            lines.add(nonItalic(messages.render("lore.owner", Placeholder.unparsed("owner", ownerName))));
        }
        return lines;
    }

    private Component nonItalic(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
