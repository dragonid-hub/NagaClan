package id.dragon.nagaclan.text;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable server-wide language snapshot; player input is never translated. */
public final class Messages {
    public static final Set<String> LANGUAGES = Set.of("en", "id", "es", "ru", "zh");
    private static volatile Map<String, String> active = Map.of();
    private static volatile boolean smallCaps;
    private static File folder;
    private Messages() {}
    public static void install(JavaPlugin plugin) {
        folder = new File(plugin.getDataFolder(), "languages");
        for (String language : LANGUAGES) {
            String path = "languages/" + language + ".yml";
            if (!new File(plugin.getDataFolder(), path).exists()) plugin.saveResource(path, false);
        }
    }
    public static void reload(String language, boolean useSmallCaps) {
        if (!LANGUAGES.contains(language)) throw new IllegalArgumentException("language must be en, id, es, ru or zh");
        Map<String, String> next = new HashMap<>(bundled("en"));
        next.putAll(bundled(language));
        File file = folder == null ? null : new File(folder, language + ".yml");
        if (file != null && file.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) { next.putAll(read(reader)); }
            catch (IOException failure) { throw new IllegalArgumentException("Cannot read language file", failure); }
        }
        active = Map.copyOf(next);
        smallCaps = useSmallCaps;
    }
    public static Map<String, String> bundled(String language) {
        try (InputStream stream = Messages.class.getResourceAsStream("/languages/" + language + ".yml")) {
            if (stream == null) throw new IllegalArgumentException("Missing bundled language: " + language);
            return read(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException failure) { throw new IllegalArgumentException("Cannot read bundled language", failure); }
    }
    private static Map<String, String> read(Reader reader) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().pathSeparator('\u0001');
        try { yaml.load(reader); }
        catch (IOException | org.bukkit.configuration.InvalidConfigurationException failure) { throw new IllegalArgumentException("Invalid language YAML", failure); }
        Map<String, String> values = new HashMap<>();
        yaml.getValues(false).forEach((key, value) -> {
            if (!(value instanceof String text)) throw new IllegalArgumentException("Language value must be text: " + key);
            values.put(key, text);
        });
        return Map.copyOf(values);
    }
    public static String get(String key) { return active.getOrDefault(key, key); }
    public static String get(String key, String fallback) { return active.getOrDefault(key, fallback); }
    public static String text(String source) { return get(source); }
    public static String usage(String command) {
        return command.replace("<nama>", get("usage.name", "<nama>")).replace("<name>", get("usage.name", "<name>"))
                .replace("<pemain>", get("usage.player", "<pemain>")).replace("<pesan>", get("usage.message", "<pesan>"));
    }
    public static String style(String text) { return smallCaps ? SmallCaps.format(text) : text; }
}
