package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.model.*;
import id.dragon.nagaclan.service.ClanService;
import id.dragon.nagaclan.text.Messages;
import id.dragon.nagaclan.text.ClanColors;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** One-use input sessions shared by dialogs and private chat fallback. */
public final class ChatPromptService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ClanService clans;
    private final Consumer<Player> reopen;
    private final ConcurrentHashMap<UUID, InputSession> pending = new ConcurrentHashMap<>();
    private final BukkitTask expiryTask;
    private volatile boolean closed;
    public ChatPromptService(JavaPlugin plugin, ClanService clans, Consumer<Player> reopen) {
        this.plugin = plugin; this.clans = clans; this.reopen = reopen;
        expiryTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                () -> pending.entrySet().removeIf(entry -> !Instant.now().isBefore(entry.getValue().expires())), 20, 20);
    }
    public void beginPrefix(Player player, Clan clan) { begin(player, clan, ClanAccess.PREFIX); }
    public void beginIcon(Player player, Clan clan) { begin(player, clan, ClanAccess.ICON); }
    private void begin(Player player, Clan clan, ClanAccess access) {
        if (closed || !plugin.isEnabled()) return;
        if (!player.hasPermission("nagaclan.settings") || clans.requireAccess(player.getUniqueId(), access) != clan) {
            throw new IllegalStateException(Messages.get("input.denied"));
        }
        InputSession session = new InputSession(UUID.randomUUID(), clan, clan.leaderId(), access, Instant.now().plusSeconds(60), false);
        pending.put(player.getUniqueId(), session);
        player.closeInventory();
        String title = Messages.get(access == ClanAccess.ICON ? "input.icon.title" : "input.prefix.title");
        if (DialogSupport.available(plugin, player)) {
            try {
                NativeInputDialog.show(player, title, access == ClanAccess.ICON ? clan.settings().icon() : clan.settings().prefix(),
                        value -> submit(player, session, value));
                return;
            } catch (RuntimeException | LinkageError failure) {
                plugin.getLogger().warning("Dialog unavailable; using chat input: " + failure.getClass().getSimpleName());
            }
        }
        pending.put(player.getUniqueId(), session.withChat());
        tell(player, Messages.get(access == ClanAccess.ICON ? "input.icon.chat" : "input.prefix.chat"));
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        InputSession session = pending.get(event.getPlayer().getUniqueId());
        if (closed || session == null || !session.chat()) return;
        id.dragon.nagaclan.listener.ClanChatPrivacy.suppressPublicDelivery(event);
        submit(event.getPlayer(), session, PlainTextComponentSerializer.plainText().serialize(event.message()));
    }
    private void submit(Player player, InputSession session, String value) {
        if (closed || !plugin.isEnabled()) return;
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                InputSession active = pending.get(player.getUniqueId());
                if (active == null || !active.token().equals(session.token()) || !pending.remove(player.getUniqueId(), active)) return;
                if (closed || !plugin.isEnabled() || !player.isOnline()) return;
                if (!Instant.now().isBefore(session.expires()) || clans.clanOf(player.getUniqueId()).orElse(null) != session.clan()
                        || !session.clan().leaderId().equals(session.leader()) || !player.hasPermission("nagaclan.settings")
                        || !session.clan().settings().allows(session.clan().roleOf(player.getUniqueId()), session.access())) {
                    tell(player, Messages.get("input.stale")); return;
                }
                String input = value == null ? "cancel" : value.strip();
                if (!input.equalsIgnoreCase("cancel")) {
                    try {
                        if (session.access() == ClanAccess.ICON) {
                            Material material = Material.matchMaterial(input.toUpperCase(java.util.Locale.ROOT));
                            if (material == null || material.isAir() || !material.isItem() || material.isLegacy()) throw new IllegalArgumentException(Messages.get("input.invalid-material"));
                            clans.setIcon(player.getUniqueId(), material.name());
                        } else clans.setPrefix(player.getUniqueId(), input);
                        tell(player, Messages.get("input.saved"));
                    } catch (IllegalArgumentException | IllegalStateException failure) { tell(player, "&#FF5C8A" + failure.getMessage()); }
                } else tell(player, Messages.get("input.cancelled"));
                reopen.accept(player);
            });
        } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { }
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { pending.remove(event.getPlayer().getUniqueId()); }
    public void clear() { pending.clear(); }
    @Override public void close() { closed = true; clear(); expiryTask.cancel(); }
    private static void tell(Player player, String message) { player.sendMessage(ClanColors.colorize(message)); }
    private record InputSession(UUID token, Clan clan, UUID leader, ClanAccess access, Instant expires, boolean chat) {
        InputSession withChat() { return new InputSession(token, clan, leader, access, expires, true); }
    }
}
