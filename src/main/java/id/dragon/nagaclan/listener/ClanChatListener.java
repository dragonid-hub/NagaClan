package id.dragon.nagaclan.listener;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.service.ClanChatService;
import id.dragon.nagaclan.service.ClanService;
import id.dragon.nagaclan.text.SmallCaps;
import id.dragon.nagaclan.text.ClanColors;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Private clan delivery. The prefix prompt cancels its input first at LOWEST priority. */
public final class ClanChatListener implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ClanService clans;
    private final ClanChatService chat;
    private volatile boolean closed;
    public ClanChatListener(JavaPlugin plugin, ClanService clans, ClanChatService chat) {
        this.plugin = plugin; this.clans = clans; this.chat = chat;
    }
    public void command(Player player, String[] args) {
        if (closed || !plugin.isEnabled()) return;
        if (!player.hasPermission("nagaclan.chat")) throw new IllegalStateException(Messages.text("Tidak memiliki permission clan chat"));
        if (args.length == 1) {
            tell(player, chat.toggle(player.getUniqueId()) ? ("&#4CC9F0" + Messages.text("Clan chat aktif. Ketik /clan chat untuk mematikan."))
                    : ("&#A78BFA" + Messages.text("Clan chat nonaktif. Chat kembali ke publik.")));
        } else {
            Clan clan = clans.clanOf(player.getUniqueId()).orElseThrow(() -> new IllegalStateException(Messages.text("Tidak memiliki clan")));
            send(player, clan, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (closed) return;
        Player player = event.getPlayer();
        Clan captured = chat.mode(player.getUniqueId()).orElse(null);
        if (captured == null) return;
        ClanChatPrivacy.suppressPublicDelivery(event);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (closed || !plugin.isEnabled() || !player.isOnline()) return;
                if (chat.mode(player.getUniqueId()).orElse(null) != captured) return;
                if (!player.hasPermission("nagaclan.chat")) {
                    chat.forget(player.getUniqueId());
                    tell(player, ("&#FF5C8A" + Messages.text("Akses clan chat dicabut.")));
                    return;
                }
                try { send(player, captured, input); }
                catch (IllegalArgumentException | IllegalStateException failure) { tell(player, "&#FF5C8A" + failure.getMessage()); }
            });
        } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { /* Plugin stopped before delivery. */ }
    }

    private void send(Player player, Clan clan, String input) {
        ClanChatService.Message delivery = chat.message(player.getUniqueId(), clan, input);
        Component message = ClanChatPrivacy.label().append(fixed("&#6D5DFB["))
                .append(Component.text(delivery.clanName(), NamedTextColor.AQUA))
                .append(fixed("&#6D5DFB] "))
                .append(Component.text(player.getName(), NamedTextColor.LIGHT_PURPLE))
                .append(fixed(" &8» "))
                .append(Component.text(delivery.body(), NamedTextColor.WHITE));
        for (var id : delivery.recipients()) {
            Player recipient = plugin.getServer().getPlayer(id);
            if (recipient != null && recipient.isOnline()) recipient.sendMessage(message);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { chat.forget(event.getPlayer().getUniqueId()); }
    @Override public void close() { closed = true; chat.clear(); }
    private static Component fixed(String value) { return LegacyComponentSerializer.legacySection().deserialize(ClanColors.colorize(Messages.style(value))); }
    private static void tell(Player player, String value) { player.sendMessage(fixed(value)); }
}
