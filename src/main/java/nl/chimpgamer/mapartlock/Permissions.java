package nl.chimpgamer.mapartlock;

import org.bukkit.command.CommandSender;

/**
 * Every permission node, all under {@code mapartlock.} to match the plugin name.
 *
 * <p>None of these carries an asterisk, and none ever should. Wildcards are a tool for the
 * server owner to grant a whole namespace at once, not something a plugin checks: a node like
 * {@code mapartlock.*} is expanded across every sibling below it, so handing it to players
 * would hand them {@link #ADMIN} too. {@link #ADMIN} is the explicit parent instead.
 */
public final class Permissions {
    public static final String USE = "mapartlock.use";
    public static final String ADMIN = "mapartlock.admin";
    public static final String BYPASS = "mapartlock.bypass";
    public static final String RELOAD = "mapartlock.reload";

    private Permissions() {
    }

    public static boolean canUse(CommandSender sender) {
        return sender.hasPermission(USE);
    }

    public static boolean canManageOthers(CommandSender sender) {
        return sender.hasPermission(ADMIN);
    }

    public static boolean canBypass(CommandSender sender) {
        return sender.hasPermission(BYPASS);
    }

    public static boolean canReload(CommandSender sender) {
        return sender.hasPermission(RELOAD);
    }
}
