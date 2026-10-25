package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.model.ClanRole;
import id.dragon.nagaclan.text.SmallCaps;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Member heads open a role-checked management page, never kick on the first click. */
public final class MembersPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    public MembersPage(ClanMenuRenderer renderer, MenuConfig config) { this.renderer = renderer; this.config = config; }

    @Override public Inventory render(Player player, Clan clan, ClanLimits limits, int requestedPage) {
        MenuListNavigation navigation = new MenuListNavigation(config, config.page("members").size());
        var slots = navigation.slots();
        List<Map.Entry<UUID, ClanRole>> members = new ArrayList<>(clan.members().entrySet());
        members.sort(Comparator.<Map.Entry<UUID, ClanRole>, ClanRole>comparing(Map.Entry::getValue)
                .thenComparing(entry -> entry.getKey().toString()));
        int pages = Math.max(1, Pagination.pageCount(members.size(), slots.size()));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        var visible = Pagination.slice(members, page, slots.size());
        var actions = navigation.actions(ClanMenuAction.OPEN_MEMBERS, page, pages);
        for (int index = 0; index < visible.size(); index++) {
            var member = visible.get(index);
            if (!RoleActionPolicy.actionsFor(clan, player.getUniqueId(), member.getKey()).isEmpty()) {
                actions.put(slots.get(index), new MenuAction(ClanMenuAction.OPEN_MEMBER, member.getKey().toString(), page));
            }
        }
        Inventory menu = renderer.create(ClanMenuPage.MEMBERS, actions);
        for (int index = 0; index < visible.size(); index++) {
            var member = visible.get(index);
            String name = Bukkit.getOfflinePlayer(member.getKey()).getName();
            if (name == null) name = member.getKey().toString();
            var item = renderer.item(Material.PLAYER_HEAD, ("&#4CC9F0&l" + Messages.text("Anggota clan")), List.of(
                    "&f" + name, Messages.style(("&7" + Messages.text("Role:") + " &f") + Messages.get("role." + member.getValue().name())),
                    Messages.style(actions.containsKey(slots.get(index)) ? ("&#A78BFA" + Messages.text("Klik untuk kelola")) : ("&7" + Messages.text("Hanya lihat")))));
            var meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(member.getKey()));
            item.setItemMeta(meta);
            menu.setItem(slots.get(index), item);
        }
        navigation.render(menu, renderer, page, pages);
        return menu;
    }
}
