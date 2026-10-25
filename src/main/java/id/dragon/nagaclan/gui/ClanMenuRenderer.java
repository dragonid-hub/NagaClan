package id.dragon.nagaclan.gui;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.text.SmallCaps;
import id.dragon.nagaclan.text.ClanColors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Applies the validated menu theme and binds actions to holder slots. */
public final class ClanMenuRenderer {
    private final MenuConfig config;

    public ClanMenuRenderer(MenuConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public Inventory create(ClanMenuPage page, Map<Integer, MenuAction> actions) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(actions, "actions");
        MenuConfig.Page layout = config.page(configPage(page));
        ClanMenuHolder holder = new ClanMenuHolder(page, actions);
        String title = layout.title();
        // Translate shipped titles; preserve an administrator's custom title verbatim.
        String defaultTail = switch (configPage(page)) {
            case "main" -> "&#4CC9F0Dashboard"; case "members" -> "&#4CC9F0Members";
            case "homes" -> "&#4CC9F0Homes"; case "bank" -> "&#A78BFABank";
            case "settings" -> "&#A78BFASettings"; case "alliances" -> "&#4CC9F0Alliances";
            default -> "&#A78BFATop Clans";
        };
        if (title.equals("&#6D5DFB&lNagaClan " + defaultTail)) {
            String key = page == ClanMenuPage.ACCESS ? "access" : page == ClanMenuPage.ICONS ? "icons" : configPage(page);
            title = "&#6D5DFB&lNagaClan &#4CC9F0" + Messages.get("menu." + key);
        }
        Inventory inventory = Bukkit.createInventory(holder, layout.size(), fixed(title));
        holder.setInventory(inventory);
        fillBorder(inventory);
        return inventory;
    }

    public ItemStack item(Material material, String fixedLabel, List<String> dynamicLore) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(fixedLabel, "fixedLabel");
        Objects.requireNonNull(dynamicLore, "dynamicLore");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(fixed(fixedLabel));
        meta.setLore(dynamicLore.stream().map(ClanColors::colorize).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inventory) {
        MenuConfig.Border border = config.border();
        for (int slot : border.slots()) {
            if (slot >= inventory.getSize()) {
                continue;
            }
            Material material = (slot & 1) == 0 ? border.primaryMaterial() : border.secondaryMaterial();
            inventory.setItem(slot, item(material, " ", List.of()));
        }
    }

    private static String configPage(ClanMenuPage page) {
        return switch (page) {
            case MAIN -> "main";
            case MEMBERS -> "members";
            case HOMES -> "homes";
            case BANK, DEPOSIT, WITHDRAW, BANK_SETTINGS -> "bank";
            case ALLIANCES -> "alliances";
            case SETTINGS, ICONS, ACCESS -> "settings";
            case TOP -> "top";
        };
    }

    private static String fixed(String value) {
        return ClanColors.colorize(Messages.style(value));
    }
}
