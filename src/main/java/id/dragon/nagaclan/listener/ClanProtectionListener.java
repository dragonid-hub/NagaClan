package id.dragon.nagaclan.listener;

import id.dragon.nagaclan.service.ClanService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class ClanProtectionListener implements Listener {
    private final ClanService clans;
    public ClanProtectionListener(ClanService clans) { this.clans = clans; }
    @EventHandler public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player target)) return;
        var first = clans.clanOf(attacker.getUniqueId()); var second = clans.clanOf(target.getUniqueId());
        if (first.isPresent() && second.isPresent() && first.get().id().equals(second.get().id()) && !first.get().settings().friendlyFire()) {
            event.setCancelled(true);
            attacker.sendActionBar(net.kyori.adventure.text.Component.text(
                    id.dragon.nagaclan.text.Messages.get("pvp.blocked"), net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
        }
    }
}
