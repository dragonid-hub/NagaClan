package id.dragon.nagaclan.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import java.util.Objects;

public final class ClanMenuListener implements Listener {
    private final ClanMenuController controller;

    public ClanMenuListener(ClanMenuController controller) {
        this.controller = Objects.requireNonNull(controller, "controller");
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ClanMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        holder.actionAt(event.getRawSlot()).ifPresent(action -> controller.handle(player, action));
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ClanMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        controller.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        controller.forget(event.getPlayer().getUniqueId());
    }

}
