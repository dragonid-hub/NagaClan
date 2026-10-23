package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.ClanMenuAction;
import id.dragon.nagaclan.gui.ClanMenuPage;
import id.dragon.nagaclan.gui.ClanMenuRenderer;
import id.dragon.nagaclan.gui.DashboardModel;
import id.dragon.nagaclan.gui.MenuAction;
import id.dragon.nagaclan.gui.MenuPageRenderer;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Main clan dashboard backed exclusively by leader-derived limits. */
public final class DashboardPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;

    public DashboardPage(ClanMenuRenderer renderer, MenuConfig config) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public Inventory render(Player player, Clan clan, ClanLimits limits, int page) {
        DashboardModel model = DashboardModel.from(clan, limits);
        Map<Integer, MenuAction> actions = new HashMap<>();
        add(actions, "main.members", ClanMenuAction.OPEN_MEMBERS);
        add(actions, "main.homes", ClanMenuAction.OPEN_HOMES);
        add(actions, "main.bank", ClanMenuAction.OPEN_BANK);
        add(actions, "main.alliances", ClanMenuAction.OPEN_ALLIANCES);
        add(actions, "main.settings", ClanMenuAction.OPEN_SETTINGS);
        add(actions, "main.top", ClanMenuAction.OPEN_TOP);
        add(actions, "main.leave", clan.roleOf(player.getUniqueId()) == id.dragon.nagaclan.model.ClanRole.LEADER
                ? ClanMenuAction.DISBAND_CONFIRM : ClanMenuAction.LEAVE);

        Inventory inventory = renderer.create(ClanMenuPage.MAIN, actions);
        inventory.setItem(13, renderer.item(Material.PLAYER_HEAD, ("&#A78BFA&l" + Messages.text("Ringkasan clan")), List.of(
                "&#4CC9F0" + clan.name(),
                fixed(("&7" + Messages.text("Level:") + " &f")) + model.level() + fixed(" &8| &7XP: &f") + model.experience(),
                fixed(("&7" + Messages.text("Anggota:") + " &f")) + model.memberCount(),
                fixed(("&7" + Messages.text("Homes:") + " &f")) + model.homeUsage(),
                fixed(("&7" + Messages.text("Bank:") + " &f")) + model.bankUsage(),
                fixed(("&7" + Messages.text("Aliansi:") + " &f")) + model.allianceCount())));
        setButton(inventory, "main.members", ("&#4CC9F0&l" + Messages.text("Anggota")), ("&7" + Messages.text("Lihat dan kelola anggota clan")));
        setButton(inventory, "main.homes", ("&#4CC9F0&l" + Messages.text("Homes")), ("&7" + Messages.text("Teleport ke home clan")));
        setButton(inventory, "main.bank", ("&#A78BFA&l" + Messages.text("Bank")), ("&7" + Messages.text("Deposit, withdraw, dan akses member")));
        setButton(inventory, "main.alliances", ("&#4CC9F0&l" + Messages.text("Aliansi")), ("&7" + Messages.text("Lihat aliansi clan")));
        setButton(inventory, "main.settings", ("&#A78BFA&l" + Messages.text("Settings")), ("&7" + Messages.text("PvP, prefix, icon, dan visibilitas")));
        setButton(inventory, "main.top", ("&#A78BFA&l" + Messages.text("Top clan")), ("&7" + Messages.text("Lihat peringkat clan")));
        boolean leader = clan.roleOf(player.getUniqueId()) == id.dragon.nagaclan.model.ClanRole.LEADER;
        setButton(inventory, "main.leave", leader ? ("&#FF5C8A&l" + Messages.text("Bubarkan clan")) : ("&#FFB86C&l" + Messages.text("Keluar clan")), ("&7" + Messages.text("Memerlukan konfirmasi terlebih dahulu")));
        return inventory;
    }

    private void add(Map<Integer, MenuAction> actions, String button, ClanMenuAction action) {
        actions.put(config.button(button).slot(), new MenuAction(action, "", 0));
    }

    private void setButton(Inventory inventory, String button, String label, String lore) {
        MenuConfig.Button value = config.button(button);
        inventory.setItem(value.slot(), renderer.item(value.material(), label, List.of(fixed(lore))));
    }

    private static String fixed(String value) { return Messages.style(value); }
}
