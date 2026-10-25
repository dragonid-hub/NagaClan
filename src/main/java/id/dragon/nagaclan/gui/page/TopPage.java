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
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TopPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    private final ClanService clans;
    public TopPage(ClanMenuRenderer renderer, MenuConfig config, ClanService clans) {
        this.renderer = renderer; this.config = config; this.clans = clans;
    }

    @Override public Inventory render(Player player, Clan viewerClan, ClanLimits limits, int requestedPage) {
        MenuListLayout layout = new MenuListLayout(config.page("top").size());
        List<Integer> slots = layout.contentSlots();
        List<Clan> ranking = TopRanking.rank(clans.allClans());
        int page = Math.max(0, Math.min(requestedPage, Pagination.pageCount(ranking.size(), slots.size()) - 1));
        List<Clan> visible = Pagination.slice(ranking, page, slots.size());
        Map<Integer, MenuAction> actions = new HashMap<>();
        actions.put(layout.back(), new MenuAction(viewerClan == null ? ClanMenuAction.CLOSE : ClanMenuAction.OPEN_DASHBOARD, "", 0));
        if (page > 0) actions.put(layout.previous(), new MenuAction(ClanMenuAction.OPEN_TOP, "", page - 1));
        if ((page + 1) * slots.size() < ranking.size()) actions.put(layout.next(), new MenuAction(ClanMenuAction.OPEN_TOP, "", page + 1));
        Inventory menu = renderer.create(ClanMenuPage.TOP, actions);
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
        for (int index = 0; index < visible.size(); index++) {
            Clan clan = visible.get(index);
            Material icon = Material.matchMaterial(clan.settings().icon());
            if (icon == null || !icon.isItem() || icon.isAir()) icon = Material.AMETHYST_SHARD;
            // Clan name and prefix are dynamic data, not small-caps template copy.
            var item = renderer.item(icon, ("&#A78BFA&l" + Messages.text("Peringkat") + " ") + (page * slots.size() + index + 1), List.of(
                    "&#4CC9F0" + clan.name(), "&f" + clan.settings().prefix(),
                    Messages.style(("&7" + Messages.text("Level:") + " &f")) + clan.level(),
                    Messages.style("&7XP: &f") + numbers.format(clan.experience()),
                    Messages.style(("&7" + Messages.text("Bank:") + " &f")) + numbers.format(clan.bank()),
                    Messages.style(("&7" + Messages.text("Anggota:") + " &f")) + clan.members().size()));
            menu.setItem(slots.get(index), item);
        }
        if (visible.isEmpty()) menu.setItem(slots.getFirst(), renderer.item(Material.BARRIER, ("&#A9B8FF" + Messages.text("Belum ada clan yang ditampilkan")), List.of()));
        menu.setItem(layout.back(), renderer.item(Material.ARROW, viewerClan == null ? "&#A9B8FFTutup" : ("&#A9B8FF" + Messages.text("Kembali")), List.of()));
        if (actions.containsKey(layout.previous())) menu.setItem(layout.previous(), renderer.item(Material.ARROW, ("&#4CC9F0" + Messages.text("Sebelumnya")), List.of()));
        if (actions.containsKey(layout.next())) menu.setItem(layout.next(), renderer.item(Material.ARROW, ("&#4CC9F0" + Messages.text("Berikutnya")), List.of()));
        return menu;
    }
}
