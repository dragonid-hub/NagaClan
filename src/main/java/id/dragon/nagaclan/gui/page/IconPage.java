package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.model.ClanRole;
import id.dragon.nagaclan.text.SmallCaps;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IconPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    public IconPage(ClanMenuRenderer renderer, MenuConfig config) { this.renderer = renderer; this.config = config; }

    @Override public Inventory render(Player player, Clan clan, ClanLimits limits, int requestedPage) {
        MenuListLayout layout = new MenuListLayout(config.page("settings").size());
        List<Integer> content = layout.contentSlots();
        int customSlot = content.getLast();
        List<Integer> slots = content.subList(0, content.size() - 1);
        int page = Math.max(0, Math.min(requestedPage, Pagination.pageCount(config.allowedIcons().size(), slots.size()) - 1));
        List<Material> icons = Pagination.slice(config.allowedIcons(), page, slots.size());
        Map<Integer, MenuAction> actions = new HashMap<>();
        if (clan.settings().allows(clan.roleOf(player.getUniqueId()), id.dragon.nagaclan.model.ClanAccess.ICON)) {
            for (int index = 0; index < icons.size(); index++) {
                actions.put(slots.get(index), new MenuAction(ClanMenuAction.SELECT_ICON, icons.get(index).name(), page));
            }
            actions.put(customSlot, new MenuAction(ClanMenuAction.CUSTOM_ICON, "", 0));
        }
        actions.putIfAbsent(layout.back(), new MenuAction(ClanMenuAction.OPEN_SETTINGS, "", 0));
        if (page > 0) actions.put(layout.previous(), new MenuAction(ClanMenuAction.OPEN_ICONS, "", page - 1));
        if ((page + 1) * slots.size() < config.allowedIcons().size()) actions.put(layout.next(), new MenuAction(ClanMenuAction.OPEN_ICONS, "", page + 1));
        Inventory menu = renderer.create(ClanMenuPage.ICONS, actions);
        for (int index = 0; index < icons.size(); index++) {
            Material icon = icons.get(index);
            menu.setItem(slots.get(index), renderer.item(icon, "&#A78BFA&l" + icon.name().replace('_', ' '),
                    List.of(Messages.style(icon.name().equals(clan.settings().icon()) ? ("&#4CC9F0" + Messages.text("Dipakai sekarang")) : ("&7" + Messages.text("Klik untuk memilih"))))));
        }
        menu.setItem(layout.back(), renderer.item(Material.ARROW, "&#A9B8FF" + Messages.text("Kembali"), List.of()));
        if (actions.containsKey(customSlot)) menu.setItem(customSlot, renderer.item(Material.NAME_TAG, ("&#A9B8FF" + Messages.text("Custom material")), List.of()));
        if (actions.containsKey(layout.previous())) menu.setItem(layout.previous(), renderer.item(Material.ARROW, ("&#4CC9F0" + Messages.text("Sebelumnya")), List.of()));
        if (actions.containsKey(layout.next())) menu.setItem(layout.next(), renderer.item(Material.ARROW, ("&#4CC9F0" + Messages.text("Berikutnya")), List.of()));
        return menu;
    }
}
