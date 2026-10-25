package id.dragon.nagaclan.command;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.limit.ClanLimitService;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.service.ClanService;
import id.dragon.nagaclan.model.ClanRole;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import id.dragon.nagaclan.gui.ClanMenuController;
import id.dragon.nagaclan.gui.ClanMenuPage;
import id.dragon.nagaclan.text.SmallCaps;
import id.dragon.nagaclan.text.ClanColors;

public final class ClanCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ClanService clans;
    private final ClanCommandLimitFlow homeLimits;
    private final ClanMenuController menus;
    private final id.dragon.nagaclan.listener.ClanChatListener chat;

    public ClanCommand(JavaPlugin plugin, ClanService clans, ClanLimitService limits, ClanMenuController menus,
                       id.dragon.nagaclan.listener.ClanChatListener chat) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.clans = Objects.requireNonNull(clans, "clans");
        this.menus = Objects.requireNonNull(menus, "menus");
        this.chat = Objects.requireNonNull(chat, "chat");
        this.homeLimits = new ClanCommandLimitFlow(clans, limits,
                task -> plugin.getServer().getScheduler().runTask(plugin, task));
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(message(("&#FF5C8A" + Messages.text("Hanya pemain yang dapat memakai command ini.")))); return true; }
        String requested = args.length == 0 ? "menu" : args[0];
        if (!ClanPermissionPolicy.allowed(requested, player::hasPermission)) {
            player.sendMessage(message(("&#FF5C8A" + Messages.text("Tidak memiliki permission untuk command ini."))));
            return true;
        }
        if (args.length == 0) { menus.open(player, ClanMenuPage.MAIN, 0); return true; }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> { require(args, 2, "/clan create <nama>"); clans.create(player.getUniqueId(), args[1]); player.sendMessage(message(("&#4CC9F0" + Messages.text("Clan dibuat.")))); }
                case "accept" -> { player.sendMessage(message(("&#4CC9F0" + Messages.text("Bergabung ke") + " ") + clans.accept(player.getUniqueId()).name())); }
                case "deny" -> { clans.deny(player.getUniqueId()); player.sendMessage(message(("&#FFB86C" + Messages.text("Undangan ditolak.")))); }
                case "leave" -> { clans.leave(player.getUniqueId()); player.sendMessage(message(("&#FFB86C" + Messages.text("Keluar dari clan.")))); }
                case "menu" -> menus.open(player, ClanMenuPage.MAIN, 0);
                case "settings" -> menus.open(player, ClanMenuPage.SETTINGS, 0);
                case "delhome" -> { require(args, 2, "/clan delhome <name>"); clans.deleteHome(player.getUniqueId(), args[1]); player.sendMessage(message(("&#4CC9F0" + Messages.text("Home deleted.")))); }
                case "level" -> { var clan=clans.clanOf(player.getUniqueId()).orElseThrow(()->new IllegalStateException(Messages.text("Tidak memiliki clan"))); player.sendMessage(message(("&#4CC9F0" + Messages.text("Level") + " &f")+clan.level()+" &8| &#A78BFAXP: &f"+clan.experience())); }
                case "chat" -> chat.command(player, args);
                case "top" -> menus.open(player, ClanMenuPage.TOP, 0);
                case "alliance", "aliance" -> { require(args,2, "/clan alliance <nama>"); clans.requestAlliance(player.getUniqueId(),args[1]); player.sendMessage(message(("&#4CC9F0" + Messages.text("Permintaan aliansi dikirim.")))); }
                case "sethome" -> {
                    require(args,2, "/clan sethome <nama>");
                    Clan clan = clans.clanOf(player.getUniqueId())
                            .orElseThrow(() -> new IllegalStateException(Messages.text("Tidak memiliki clan")));
                    var location = player.getLocation();
                    if (location.getWorld() == null) throw new IllegalStateException(Messages.text("World tidak tersedia"));
                    String serialized = location.getWorld().getName()+","+location.getX()+","+location.getY()+","+location.getZ()+","+location.getYaw()+","+location.getPitch();
                    homeLimits.setHome(player.getUniqueId(), clan.id(), args[1], serialized,
                            () -> plugin.isEnabled() && player.isOnline()
                                    && player.hasPermission(ClanPermissionPolicy.command("sethome")),
                            () -> player.sendMessage(message(("&#4CC9F0" + Messages.text("Home clan disetel.")))),
                            failure -> player.sendMessage(message("&#FF5C8A" + failure)));
                }
                case "home" -> { require(args,2, "/clan home <nama>"); String[] p=clans.home(player.getUniqueId(),args[1]).orElseThrow(()->new IllegalStateException(Messages.text("Home tidak ditemukan"))).split(","); var world=player.getServer().getWorld(p[0]); if(world==null) throw new IllegalStateException(Messages.text("World home tidak ditemukan")); player.teleport(new org.bukkit.Location(world,Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3]),Float.parseFloat(p[4]),Float.parseFloat(p[5]))); }
                case "disband", "disban" -> { if (clans.confirmDisband(player.getUniqueId())) player.sendMessage(message(("&#FF5C8A" + Messages.text("Clan dibubarkan.")))); else player.sendMessage(message(("&#FFB86C" + Messages.text("Jalankan /clan disband lagi dalam 30 detik.")))); }
                case "invite" -> { require(args, 2, "/clan invite <pemain>"); Player target=player.getServer().getPlayerExact(args[1]); if(target==null) throw new IllegalArgumentException(Messages.text("Pemain harus online.")); clans.invite(player.getUniqueId(),target.getUniqueId(),"",Duration.ofSeconds(60)); player.sendMessage(message(("&#4CC9F0" + Messages.text("Undangan dikirim.")))); }
                case "kick" -> { Player target=requireOnlineTarget(player,args,"/clan kick <pemain>"); clans.kick(player.getUniqueId(),target.getUniqueId()); player.sendMessage(message(("&#FFB86C" + Messages.text("Anggota dikeluarkan.")))); }
                case "setbuilder" -> { Player target=requireOnlineTarget(player,args,"/clan setbuilder <pemain>"); clans.setRole(player.getUniqueId(),target.getUniqueId(),ClanRole.BUILDER); player.sendMessage(message(("&#4CC9F0" + Messages.text("Role diubah menjadi builder.")))); }
                case "setadmin" -> { Player target=requireOnlineTarget(player,args,"/clan setadmin <pemain>"); clans.setRole(player.getUniqueId(),target.getUniqueId(),ClanRole.ADMIN); player.sendMessage(message(("&#A78BFA" + Messages.text("Role diubah menjadi admin.")))); }
                case "setleader" -> { Player target=requireOnlineTarget(player,args,"/clan setleader <pemain>"); clans.setRole(player.getUniqueId(),target.getUniqueId(),ClanRole.LEADER); player.sendMessage(message(("&#8B7BFF" + Messages.text("Leadership dipindahkan.")))); }
                default -> player.sendMessage(message("&#A9B8FFGunakan &f/clan menu&#A9B8FF."));
            }
        } catch (IllegalArgumentException | IllegalStateException ex) { player.sendMessage(message("&#FF5C8A" + ex.getMessage())); }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        var currentClan = clans.clanOf(player.getUniqueId());
        List<String> base;
        if (currentClan.isEmpty()) {
            base = List.of("create", "accept", "deny");
        } else {
            ClanRole role = currentClan.get().roleOf(player.getUniqueId());
            List<String> allowed = new ArrayList<>(List.of("leave", "top", "level", "home", "menu", "settings"));
            if (player.hasPermission("nagaclan.chat")) allowed.add("chat");
            allowed.addAll(List.of("sethome", "delhome", "invite", "kick", "setbuilder", "setadmin", "alliance"));
            if (role == ClanRole.LEADER) allowed.addAll(List.of("setleader", "disband"));
            allowed.removeIf(value -> { var access = id.dragon.nagaclan.model.ClanAccess.command(value); return access != null && !currentClan.get().settings().allows(role, access); });
            base = List.copyOf(allowed);
        }
        base = base.stream().filter(value -> ClanPermissionPolicy.allowed(value, player::hasPermission)).toList();
        if (args.length == 1) return base.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if (args.length == 2) {
            String canonical = switch (args[0].toLowerCase(Locale.ROOT)) {
                case "aliance" -> "alliance";
                case "disban" -> "disband";
                default -> args[0].toLowerCase(Locale.ROOT);
            };
            if (!base.contains(canonical)) return List.of();
            var clan = clans.clanOf(player.getUniqueId());
            List<String> homes = clan.map(value -> value.homes().keySet().stream().sorted().toList()).orElse(List.of());
            List<String> clanNames = clans.allClans().stream().filter(value -> clan.isEmpty() || !value.id().equals(clan.get().id())).map(value -> value.name()).sorted().toList();
            boolean invite = args[0].equalsIgnoreCase("invite");
            List<String> players = player.getServer().getOnlinePlayers().stream()
                    .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                    .filter(target -> invite ? clans.clanOf(target.getUniqueId()).isEmpty() : clan.map(value -> value.hasMember(target.getUniqueId())).orElse(false))
                    .map(Player::getName).sorted().toList();
            return ClanCommandSuggestions.secondArgument(args[0], homes, clanNames, players).stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
    private static void require(String[] args,int size, String usage){if(args.length<size) throw new IllegalArgumentException(Messages.text("Gunakan") + " " + Messages.usage(usage));}
    private static Player requireOnlineTarget(Player actor, String[] args, String usage) { require(args,2,usage); Player target=actor.getServer().getPlayerExact(args[1]); if(target==null) throw new IllegalArgumentException(Messages.text("Pemain harus online.")); return target; }
    private static String message(String input) { return ClanColors.colorize(Messages.style(input)); }
}

/** Coordinates the asynchronous limit lookup without exposing Bukkit state to focused tests. */
final class ClanCommandLimitFlow {
    private static final String LIMIT_FAILURE = Messages.text("Gagal membaca limit leader.");

    private final ClanService clans;
    private final ClanLimitService limits;
    private final Consumer<Runnable> syncScheduler;

    ClanCommandLimitFlow(ClanService clans, ClanLimitService limits, Consumer<Runnable> syncScheduler) {
        this.clans = Objects.requireNonNull(clans, "clans");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.syncScheduler = Objects.requireNonNull(syncScheduler, "syncScheduler");
    }

    void setHome(
            UUID actor,
            String expectedClanId,
            String name,
            String serializedLocation,
            BooleanSupplier online,
            Runnable success,
            Consumer<String> failure) {
        Clan captured = clans.clanOf(actor)
                .filter(clan -> clan.id().equals(expectedClanId))
                .orElse(null);
        if (captured == null) {
            failure.accept(Messages.text("Tidak memiliki clan"));
            return;
        }

        try {
            UUID capturedLeaderId = captured.leaderId();
            long capturedGeneration = limits.generation();
            limits.resolve(captured).whenComplete((resolved, resolutionFailure) ->
                    syncScheduler.accept(() -> {
                        if (!online.getAsBoolean()) return;
                        Clan current = clans.clanOf(actor)
                                .filter(clan -> clan.id().equals(expectedClanId))
                                .orElse(null);
                        if (current == null || !current.leaderId().equals(capturedLeaderId)) return;
                        if (resolutionFailure != null || resolved == null || capturedGeneration != limits.generation()) {
                            failure.accept(LIMIT_FAILURE);
                            return;
                        }
                        try {
                            clans.setHome(actor, name, serializedLocation, resolved.homes());
                            success.run();
                        } catch (IllegalArgumentException | IllegalStateException mutationFailure) {
                            failure.accept(mutationFailure.getMessage());
                        }
                    }));
        } catch (RuntimeException resolutionFailure) {
            syncScheduler.accept(() -> {
                if (online.getAsBoolean()) failure.accept(LIMIT_FAILURE);
            });
        }
    }
}
