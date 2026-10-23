package id.dragon.nagaclan.gui;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** No dialog classes in signatures: safe to load on older Paper versions. */
public final class DialogSupport {
    private DialogSupport() {}
    public static boolean supported(boolean apiAvailable, int clientProtocol) { return apiAvailable && clientProtocol >= 771 && clientProtocol < 0x40000000; }
    public static boolean available(JavaPlugin plugin, Player player) {
        if (!plugin.getConfig().getBoolean("dialogs.enabled", true)) return false;
        try {
            Class.forName("io.papermc.paper.dialog.Dialog", false, plugin.getClass().getClassLoader());
            int protocol = player.getProtocolVersion();
            if (plugin.getServer().getPluginManager().isPluginEnabled("ViaVersion")) {
                Class<?> via = Class.forName("com.viaversion.viaversion.api.Via");
                Object api = via.getMethod("getAPI").invoke(null);
                Class<?> apiType = Class.forName("com.viaversion.viaversion.api.ViaAPI");
                protocol = (int) apiType.getMethod("getPlayerVersion", java.util.UUID.class).invoke(api, player.getUniqueId());
            } else if (plugin.getConfig().getBoolean("dialogs.proxy-translation", false)) {
                return false; // Original client protocol cannot be established behind a translating proxy.
            }
            return supported(true, protocol);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unknown) { return false; }
    }
}
