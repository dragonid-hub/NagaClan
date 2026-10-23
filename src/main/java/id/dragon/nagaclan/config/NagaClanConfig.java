package id.dragon.nagaclan.config;
import id.dragon.nagaclan.text.Messages;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.function.Consumer;

/** Owns the atomically replaceable, validated configuration snapshot. */
public final class NagaClanConfig {
    private final java.util.function.Supplier<FileConfiguration> source;
    private final Consumer<String> warningConsumer;
    private volatile Snapshot snapshot;

    public NagaClanConfig(JavaPlugin plugin) {
        this(plugin, warning -> plugin.getLogger().warning(warning));
    }

    public NagaClanConfig(JavaPlugin plugin, Consumer<String> warningConsumer) {
        this(() -> read(plugin), warningConsumer);
    }

    public NagaClanConfig(java.util.function.Supplier<FileConfiguration> source, Consumer<String> warningConsumer) {
        this.source = Objects.requireNonNull(source, "source");
        this.warningConsumer = Objects.requireNonNull(warningConsumer, "warningConsumer");
        reload();
    }

    public void reload() {
        FileConfiguration root = Objects.requireNonNull(source.get(), "configuration source");
        ClanLimitConfig limits = ClanLimitConfig.from(root);
        MenuConfig menus = MenuConfig.from(root, warningConsumer);
        Messages.reload(root.getString("language", "id"), root.getBoolean("text.small-caps", false));
        snapshot = new Snapshot(limits, menus);
    }

    private static FileConfiguration read(JavaPlugin plugin) {
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        try {
            // load(File) throws on malformed YAML; loadConfiguration(File) would silently return empty data.
            yaml.load(new java.io.File(plugin.getDataFolder(), "config.yml"));
            return yaml;
        } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException failure) {
            throw new IllegalArgumentException("Tidak dapat membaca config.yml; pengaturan aktif dipertahankan", failure);
        }
    }

    public ClanLimitConfig limits() {
        return requireSnapshot().limits();
    }

    public MenuConfig menus() {
        return requireSnapshot().menus();
    }

    private Snapshot requireSnapshot() {
        Snapshot current = snapshot;
        if (current == null) {
            throw new IllegalStateException("NagaClan configuration has not been loaded");
        }
        return current;
    }

    private record Snapshot(ClanLimitConfig limits, MenuConfig menus) {
    }
}
