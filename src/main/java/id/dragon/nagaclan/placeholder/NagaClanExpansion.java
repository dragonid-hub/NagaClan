package id.dragon.nagaclan.placeholder;

import id.dragon.nagaclan.text.ClanColors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** Internal optional expansion; intentionally isolated from the plugin's mandatory classes. */
public final class NagaClanExpansion extends PlaceholderExpansion {
    private final ClanPlaceholderSnapshot snapshot;
    private final String version;
    public NagaClanExpansion(ClanPlaceholderSnapshot snapshot, String version) { this.snapshot = snapshot; this.version = version; }
    @Override public String getIdentifier() { return "nagaclan"; }
    @Override public String getAuthor() { return "NagaDEV"; }
    @Override public String getVersion() { return version; }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player, String params) {
        String value = snapshot.value(player == null ? null : player.getUniqueId(), params);
        return value == null ? null : ClanColors.colorize(value);
    }
}
