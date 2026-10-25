package id.dragon.nagaclan.command;
import id.dragon.nagaclan.text.Messages;

import org.bukkit.command.*;
import id.dragon.nagaclan.config.NagaClanReloadService;
import id.dragon.nagaclan.text.SmallCaps;
import id.dragon.nagaclan.service.ClanService;
import id.dragon.nagaclan.text.ClanColors;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final Supplier<String> version;
    private final ClanService clans;
    private final NagaClanReloadService reload;
    public AdminCommand(Supplier<String> version, ClanService clans, NagaClanReloadService reload) {
        this.version = version; this.clans = clans; this.reload = reload;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "version" : args[0].toLowerCase(Locale.ROOT);
        if (!sender.hasPermission("nagaclan.admin." + sub)) {
            sender.sendMessage(message(("&#FF5C8A" + Messages.text("Tidak memiliki permission."))));
            return true;
        }
        try {
            switch (sub) {
                case "reload" -> reload.reload().thenAccept(result ->
                        sender.sendMessage(message((result.successful() ? "&#4CC9F0" : "&#FF5C8A") + result.message())));
                // Config is edited on disk and owned by NagaClanConfig. Never overwrite it with JavaPlugin's stale copy.
                case "save" -> { clans.saveAll(); sender.sendMessage(message(("&#4CC9F0" + Messages.text("Data clan disimpan.")))); }
                case "version" -> sender.sendMessage(message("&#A78BFANagaClan &f" + version.get()));
                default -> sender.sendMessage(message("&#A9B8FF/nc <reload|save|version>"));
            }
        } catch (IllegalArgumentException | IllegalStateException failure) {
            sender.sendMessage(message("&#FF5C8A" + failure.getMessage()));
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return List.of("reload", "save", "version").stream()
                .filter(value -> sender.hasPermission("nagaclan.admin." + value))
                .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
    }
    private static String message(String input) { return ClanColors.colorize(Messages.style(input)); }
}
