package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.text.ClanColors;
import id.dragon.nagaclan.text.Messages;
import id.dragon.nagaclan.text.SmallCaps;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** Optional Floodgate/Cumulus bridge; no Bedrock library is bundled in NagaClan. */
final class BedrockForms {
    private static final Map<String, String> ITEM_TEXTURES = Map.ofEntries(
            Map.entry("GRASS_BLOCK", "textures/blocks/grass_side_carried"),
            Map.entry("PLAYER_HEAD", "textures/items/skull_steve"),
            Map.entry("DIAMOND", "textures/items/diamond"),
            Map.entry("EMERALD", "textures/items/emerald"),
            Map.entry("GOLD_INGOT", "textures/items/gold_ingot"),
            Map.entry("ENDER_PEARL", "textures/items/ender_pearl"),
            Map.entry("NAME_TAG", "textures/items/name_tag"),
            Map.entry("ARROW", "textures/items/arrow"),
            Map.entry("PAPER", "textures/items/paper"),
            Map.entry("NETHER_STAR", "textures/items/nether_star"));

    private BedrockForms() { }

    static boolean isBedrock(JavaPlugin plugin, Player player) {
        if (!plugin.getConfig().getBoolean("bedrock.forms", true)) return false;
        Plugin floodgate = plugin.getServer().getPluginManager().getPlugin("floodgate");
        if (floodgate == null || !floodgate.isEnabled()) return false;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi", false,
                    floodgate.getClass().getClassLoader());
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return Boolean.TRUE.equals(apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, player.getUniqueId()));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            return false;
        }
    }

    static boolean showMenu(JavaPlugin plugin, Player player, Clan clan, Inventory inventory, Consumer<MenuAction> click) {
        if (!isBedrock(plugin, player)) return false;
        try {
            ClassLoader loader = loader(plugin);
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            builderClass.getMethod("title", String.class).invoke(builder, "NagaClan · " + Messages.get("menu." + pageKey(((ClanMenuHolder) inventory.getHolder()).page())));
            StringBuilder summary = new StringBuilder(clan == null ? Messages.get("dialog.no-clan") : clan.name());
            ClanMenuHolder holder = (ClanMenuHolder) inventory.getHolder();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                if (holder.actionAt(slot).isPresent()) continue;
                ItemStack item = inventory.getItem(slot);
                if (item == null || !item.hasItemMeta()) continue;
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasDisplayName() || !meta.hasLore()) continue;
                summary.append("\n").append(plain(meta.getDisplayName()));
                for (String line : meta.getLore()) summary.append("\n").append(plain(line));
                break;
            }
            builderClass.getMethod("content", String.class).invoke(builder, summary.toString());
            Class<?> imageType = Class.forName("org.geysermc.cumulus.util.FormImage$Type", true, loader);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object pathType = Enum.valueOf((Class<Enum>) imageType.asSubclass(Enum.class), "PATH");
            Method imageButton = builderClass.getMethod("button", String.class, imageType, String.class, Consumer.class);
            Method textButton = builderClass.getMethod("button", String.class, Consumer.class);
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                MenuAction action = holder.actionAt(slot).orElse(null);
                ItemStack item = inventory.getItem(slot);
                if (action == null || action.type() == ClanMenuAction.NONE || action.type() == ClanMenuAction.CLOSE
                        || item == null || item.getType().isAir()) continue;
                ItemMeta meta = item.getItemMeta();
                String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
                String label = plain(DialogButtonText.label(action.type(), name));
                Consumer<Object> selected = ignored -> click.accept(action);
                String texture = ITEM_TEXTURES.get(item.getType().name());
                if (texture == null) textButton.invoke(builder, label, selected);
                else imageButton.invoke(builder, label, pathType, texture, selected);
            }
            textButton.invoke(builder, SmallCaps.format(Messages.get("dialog.close")), (Consumer<Object>) ignored -> { });
            Object form = builderClass.getMethod("build").invoke(builder);
            return send(plugin, player.getUniqueId(), form);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            plugin.getLogger().warning("Bedrock form unavailable; using inventory GUI: " + failure.getClass().getSimpleName());
            return false;
        }
    }

    static boolean showInput(JavaPlugin plugin, Player player, String title, String initial, Consumer<String> submit) {
        if (!isBedrock(plugin, player)) return false;
        try {
            ClassLoader loader = loader(plugin);
            Class<?> formClass = Class.forName("org.geysermc.cumulus.form.CustomForm", true, loader);
            Object builder = formClass.getMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            builderClass.getMethod("title", String.class).invoke(builder, "NagaClan · " + title);
            builderClass.getMethod("input", String.class, String.class, String.class)
                    .invoke(builder, title, title, initial == null ? "" : initial);
            builderClass.getMethod("validResultHandler", Consumer.class).invoke(builder,
                    (Consumer<Object>) response -> {
                        try { submit.accept((String) response.getClass().getMethod("asInput", int.class).invoke(response, 0)); }
                        catch (ReflectiveOperationException | RuntimeException ignored) { submit.accept(null); }
                    });
            Object form = builderClass.getMethod("build").invoke(builder);
            return send(plugin, player.getUniqueId(), form);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            plugin.getLogger().warning("Bedrock input form unavailable; using chat input: " + failure.getClass().getSimpleName());
            return false;
        }
    }

    private static boolean send(JavaPlugin plugin, UUID playerId, Object form) throws ReflectiveOperationException {
        ClassLoader loader = loader(plugin);
        Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, loader);
        Class<?> formType = Class.forName("org.geysermc.cumulus.form.Form", true, loader);
        Object api = apiClass.getMethod("getInstance").invoke(null);
        Object result = apiClass.getMethod("sendForm", UUID.class, formType).invoke(api, playerId, form);
        return !Boolean.FALSE.equals(result);
    }

    private static ClassLoader loader(JavaPlugin plugin) {
        return plugin.getServer().getPluginManager().getPlugin("floodgate").getClass().getClassLoader();
    }

    private static String plain(String text) {
        String stripped = ChatColor.stripColor(ClanColors.colorize(text));
        return stripped == null ? text : stripped;
    }

    private static String pageKey(ClanMenuPage page) {
        return switch (page) {
            case MAIN -> "main";
            case MEMBERS -> "members";
            case HOMES -> "homes";
            case BANK, DEPOSIT, WITHDRAW, BANK_SETTINGS -> "bank";
            case ALLIANCES -> "alliances";
            case SETTINGS -> "settings";
            case ACCESS -> "access";
            case ICONS -> "icons";
            case TOP -> "top";
        };
    }
}
