package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.*;

/** Leader-controlled role thresholds. Server permissions and role hierarchy still apply. */
public final class AccessPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    public AccessPage(ClanMenuRenderer renderer, MenuConfig config) { this.renderer = renderer; this.config = config; }
    @Override public Inventory render(Player player, Clan clan, ClanLimits limits, int requestedPage) {
        var layout = new MenuListLayout(config.page("settings").size());
        var all = List.of(ClanAccess.values());
        int page = Math.min(requestedPage, Pagination.pageCount(all.size(), layout.contentSlots().size()) - 1);
        var entries = Pagination.slice(all, page, layout.contentSlots().size());
        Map<Integer, MenuAction> actions = new HashMap<>();
        boolean leader = clan.roleOf(player.getUniqueId()) == ClanRole.LEADER;
        for (int i = 0; i < entries.size(); i++) if (leader) actions.put(layout.contentSlots().get(i), new MenuAction(ClanMenuAction.CYCLE_ACCESS, entries.get(i).name(), page));
        actions.put(layout.back(), new MenuAction(ClanMenuAction.OPEN_SETTINGS, "", 0));
        if (page > 0) actions.put(layout.previous(), new MenuAction(ClanMenuAction.OPEN_ACCESS, "", page - 1));
        if (page + 1 < Pagination.pageCount(all.size(), layout.contentSlots().size())) actions.put(layout.next(), new MenuAction(ClanMenuAction.OPEN_ACCESS, "", page + 1));
        Inventory menu = renderer.create(ClanMenuPage.ACCESS, actions);
        for (int i = 0; i < entries.size(); i++) {
            var action = entries.get(i);
            menu.setItem(layout.contentSlots().get(i), renderer.item(Material.TRIPWIRE_HOOK,
                    "&#4CC9F0" + Messages.get("access." + action.name()), List.of(
                    "&#A78BFA" + Messages.get("role." + clan.settings().access(action).name()),
                    Messages.get(leader ? "access.click" : "access.readonly"),
                    Messages.get("access.hierarchy"))));
        }
        menu.setItem(layout.back(), renderer.item(Material.ARROW, ("&#A9B8FF" + Messages.text("Kembali")), List.of()));
        if (page > 0) menu.setItem(layout.previous(), renderer.item(Material.ARROW, ("&#4CC9F0" + Messages.text("Sebelumnya")), List.of()));
        if (actions.containsKey(layout.next())) menu.setItem(layout.next(), renderer.item(Material.ARROW, ("&#4CC9F0" + Messages.text("Berikutnya")), List.of()));
        return menu;
    }
}
