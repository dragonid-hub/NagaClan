package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.model.ClanRole;
import id.dragon.nagaclan.model.ClanAccess;
import id.dragon.nagaclan.text.SmallCaps;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SettingsPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    public SettingsPage(ClanMenuRenderer renderer, MenuConfig config) { this.renderer = renderer; this.config = config; }

    public static Map<Integer, MenuAction> actions(Clan clan, UUID viewer, int size) {
        new MenuListLayout(size); // Validate before choosing compact slots.
        Map<Integer, MenuAction> actions = new HashMap<>();
        int[] slots = slots(size);
        if (clan.settings().allows(clan.roleOf(viewer), ClanAccess.PVP)) {
            actions.put(slots[0], new MenuAction(ClanMenuAction.TOGGLE_FRIENDLY_FIRE, "", 0));
        }
        if (clan.settings().allows(clan.roleOf(viewer), ClanAccess.TOP_VISIBILITY)) {
            actions.put(slots[1], new MenuAction(ClanMenuAction.TOGGLE_TOP_VISIBILITY, "", 0));
        }
        if (clan.settings().allows(clan.roleOf(viewer), ClanAccess.PREFIX)) {
            actions.put(slots[2], new MenuAction(ClanMenuAction.EDIT_PREFIX, "", 0));
        }
        if (clan.settings().allows(clan.roleOf(viewer), ClanAccess.ICON)) {
            actions.put(slots[3], new MenuAction(ClanMenuAction.OPEN_ICONS, "", 0));
        }
        actions.put(size >= 45 ? 34 : 5, new MenuAction(ClanMenuAction.OPEN_ACCESS, "", 0));
        actions.put(slots[4], new MenuAction(ClanMenuAction.OPEN_TOP, "", 0));
        actions.put(size - 1, new MenuAction(ClanMenuAction.OPEN_DASHBOARD, "", 0));
        return Map.copyOf(actions);
    }

    @Override public Inventory render(Player player, Clan clan, ClanLimits limits, int page) {
        int size = config.page("settings").size();
        int[] slots = slots(size);
        Inventory menu = renderer.create(ClanMenuPage.SETTINGS, actions(clan, player.getUniqueId(), size));
        menu.setItem(slots[0], renderer.item(clan.settings().friendlyFire() ? Material.LIME_DYE : Material.RED_DYE,
                ("&#4CC9F0&l" + Messages.text("PvP antar anggota")), List.of(state(clan.settings().friendlyFire()), access(clan, player, ClanAccess.PVP))));
        menu.setItem(slots[1], renderer.item(Material.NETHER_STAR, ("&#A78BFA&l" + Messages.text("Tampil di top clan")),
                List.of(state(clan.settings().visibleOnTop()), access(clan, player, ClanAccess.TOP_VISIBILITY))));
        menu.setItem(slots[2], renderer.item(Material.NAME_TAG, ("&#4CC9F0&l" + Messages.text("Prefix clan")),
                List.of("&f" + clan.settings().prefix(), Messages.style(("&7" + Messages.text("1-12 karakter terlihat; mendukung warna"))), access(clan, player, ClanAccess.PREFIX))));
        Material icon = Material.matchMaterial(clan.settings().icon());
        if (icon == null || !icon.isItem() || icon.isAir()) icon = Material.AMETHYST_SHARD;
        menu.setItem(slots[3], renderer.item(icon, ("&#A78BFA&l" + Messages.text("Icon clan")), List.of(access(clan, player, ClanAccess.ICON))));
        menu.setItem(slots[4], renderer.item(Material.DIAMOND, ("&#4CC9F0&l" + Messages.text("Lihat top clan")),
                List.of(Messages.style(("&7" + Messages.text("Peringkat level, XP, lalu bank"))))));
        menu.setItem(size - 1, renderer.item(Material.ARROW, ("&#A9B8FF" + Messages.text("Kembali")), List.of()));
        menu.setItem(size >= 45 ? 34 : 5, renderer.item(Material.TRIPWIRE_HOOK, ("&#A78BFA" + Messages.text("Access settings")), List.of()));
        return menu;
    }

    private static int[] slots(int size) { return size >= 45 ? new int[]{20, 22, 24, 30, 32} : new int[]{0, 1, 2, 3, 4}; }
    private static String access(Clan clan, Player player, ClanAccess action) {
        return "&#A9B8FF" + Messages.text(clan.settings().allows(clan.roleOf(player.getUniqueId()), action) ? "Klik untuk mengubah" : "Hanya lihat");
    }
    private static String state(boolean enabled) { return Messages.style(enabled ? ("&#4CC9F0" + Messages.text("Aktif")) : ("&#FF5C8A" + Messages.text("Nonaktif"))); }
}
