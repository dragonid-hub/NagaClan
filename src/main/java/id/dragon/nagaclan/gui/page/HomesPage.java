package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.text.SmallCaps;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.List;

/** Home actions carry names so execution resolves the latest saved location. */
public final class HomesPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    public HomesPage(ClanMenuRenderer renderer, MenuConfig config) { this.renderer = renderer; this.config = config; }

    @Override public Inventory render(Player player, Clan clan, ClanLimits limits, int requestedPage) {
        MenuListNavigation navigation = new MenuListNavigation(config, config.page("homes").size());
        var slots = navigation.slots();
        var names = clan.homes().keySet().stream().sorted().toList();
        int pages = Math.max(1, Pagination.pageCount(names.size(), slots.size()));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        var visible = Pagination.slice(names, page, slots.size());
        var actions = navigation.actions(ClanMenuAction.OPEN_HOMES, page, pages);
        for (int index = 0; index < visible.size(); index++) {
            actions.put(slots.get(index), new MenuAction(ClanMenuAction.HOME_TELEPORT, visible.get(index), page));
        }
        Inventory menu = renderer.create(ClanMenuPage.HOMES, actions);
        for (int index = 0; index < visible.size(); index++) {
            menu.setItem(slots.get(index), renderer.item(Material.ENDER_PEARL, ("&#4CC9F0&l" + Messages.text("Home clan")), List.of(
                    "&f" + visible.get(index), Messages.style(("&#A9B8FF" + Messages.text("Klik untuk teleport"))),
                    Messages.style(("&7" + Messages.text("Home:") + " &f")) + names.size() + " / " + limits.homes())));
        }
        if (visible.isEmpty()) menu.setItem(slots.getFirst(), renderer.item(Material.BARRIER,
                ("&#A78BFA" + Messages.text("Belum ada home")), List.of(Messages.style(("&7" + Messages.text("Gunakan") + " ")) + Messages.usage("&f/clan sethome <nama>"))));
        navigation.render(menu, renderer, page, pages);
        return menu;
    }
}
