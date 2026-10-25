package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.model.Clan;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class MemberActionsPage {
    private MemberActionsPage() {}
    public static Inventory render(Player player, Clan clan, UUID target, int page, MenuConfig config) {
        var targetRole = clan.roleOf(target);
        if (targetRole == null) throw new IllegalStateException(Messages.text("Anggota sudah tidak berada di clan"));
        var allowed = RoleActionPolicy.actionsFor(clan, player.getUniqueId(), target);
        if (allowed.isEmpty()) throw new IllegalStateException(Messages.text("Tidak dapat mengelola anggota ini"));
        int size = config.page("members").size();
        int slot = size >= 27 ? 10 : 0;
        var actions = new HashMap<Integer, MenuAction>();
        for (var type : allowed.stream().sorted().toList()) actions.put(slot++, new MenuAction(type, target.toString(), page));
        actions.put(size - 1, new MenuAction(ClanMenuAction.OPEN_MEMBERS, "", page));
        var renderer = new ClanMenuRenderer(config);
        var menu = renderer.create(ClanMenuPage.MEMBERS, actions);
        String name = player.getServer().getOfflinePlayer(target).getName();
        String display = name == null ? target.toString() : name;
        actions.forEach((position, action) -> menu.setItem(position, renderer.item(material(action.type()), label(action.type()), List.of("&f" + display))));
        return menu;
    }
    private static Material material(ClanMenuAction type) {
        return switch (type) {
            case MEMBER_KICK -> Material.BARRIER;
            case SET_BUILDER -> Material.IRON_PICKAXE;
            case SET_ADMIN -> Material.SHIELD;
            case SET_LEADER -> Material.NETHER_STAR;
            default -> Material.ARROW;
        };
    }
    private static String label(ClanMenuAction type) {
        return switch (type) {
            case MEMBER_KICK -> ("&#FF5C8A" + Messages.text("Keluarkan anggota"));
            case SET_BUILDER -> ("&#4CC9F0" + Messages.text("Jadikan builder"));
            case SET_ADMIN -> ("&#A78BFA" + Messages.text("Jadikan admin"));
            case SET_LEADER -> ("&#FFB86C" + Messages.text("Transfer leader"));
            default -> ("&#A9B8FF" + Messages.text("Kembali"));
        };
    }
}
