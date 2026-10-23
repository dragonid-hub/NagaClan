package id.dragon.nagaclan.placeholder;

import id.dragon.nagaclan.service.ClanService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Instantiated only when PlaceholderAPI is enabled; no network requests or PAPI downloads at runtime. */
public final class PlaceholderHook implements AutoCloseable {
    private final ClanPlaceholderSnapshot snapshot = new ClanPlaceholderSnapshot();
    private final NagaClanExpansion expansion;
    private final BukkitTask refresh;
    public PlaceholderHook(JavaPlugin plugin, ClanService clans) {
        snapshot.refresh(clans.allClans());
        expansion = new NagaClanExpansion(snapshot, plugin.getPluginMeta().getVersion());
        if (!expansion.register()) throw new IllegalStateException("Identifier nagaclan sudah terdaftar di PlaceholderAPI");
        try {
            refresh = plugin.getServer().getScheduler().runTaskTimer(plugin,
                    () -> snapshot.refresh(clans.allClans()), 20L, 20L);
        } catch (RuntimeException failure) {
            expansion.unregister();
            throw failure;
        }
    }
    @Override public void close() {
        refresh.cancel();
        snapshot.clear();
        expansion.unregister();
    }
}
