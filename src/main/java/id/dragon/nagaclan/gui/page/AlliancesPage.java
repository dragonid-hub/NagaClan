package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.service.ClanService;
import id.dragon.nagaclan.text.SmallCaps;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.Comparator;
import java.util.List;

/** Shows alliances already stored by ClanService; does not invent reciprocal membership. */
public final class AlliancesPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    private final ClanService clans;
    public AlliancesPage(ClanMenuRenderer renderer, MenuConfig config, ClanService clans) {
        this.renderer = renderer; this.config = config; this.clans = clans;
    }
    @Override public Inventory render(Player player, Clan clan, ClanLimits limits, int requestedPage) {
        var navigation = new MenuListNavigation(config, config.page("alliances").size());
        var slots = navigation.slots();
        var allies = clans.allClans().stream().filter(other -> clan.alliances().contains(other.id()))
                .sorted(Comparator.comparing(Clan::name, String.CASE_INSENSITIVE_ORDER)).toList();
        int pages = Math.max(1, Pagination.pageCount(allies.size(), slots.size()));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        var visible = Pagination.slice(allies, page, slots.size());
        Inventory menu = renderer.create(ClanMenuPage.ALLIANCES, navigation.actions(ClanMenuAction.OPEN_ALLIANCES, page, pages));
        for (int index = 0; index < visible.size(); index++) {
            Clan ally = visible.get(index);
            menu.setItem(slots.get(index), renderer.item(Material.HEART_OF_THE_SEA, ("&#4CC9F0&l" + Messages.text("Aliansi clan")), List.of(
                    "&f" + ally.name(), Messages.style(("&7" + Messages.text("Level:") + " &f")) + ally.level(),
                    Messages.style(("&7" + Messages.text("Anggota:") + " &f")) + ally.members().size())));
        }
        if (visible.isEmpty()) menu.setItem(slots.getFirst(), renderer.item(Material.PAPER, ("&#A78BFA" + Messages.text("Belum ada aliansi")),
                List.of(Messages.style("&7" + Messages.text("Gunakan") + " ") + Messages.usage("&f/clan alliance <nama>"))));
        navigation.render(menu, renderer, page, pages);
        return menu;
    }
}
