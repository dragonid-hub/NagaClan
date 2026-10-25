package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/** Renders one routed menu page. Feature pages are supplied in later tasks. */
@FunctionalInterface
public interface MenuPageRenderer {
    Inventory render(Player player, Clan clan, ClanLimits limits, int page);
}
