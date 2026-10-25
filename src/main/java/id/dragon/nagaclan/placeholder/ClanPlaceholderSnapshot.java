package id.dragon.nagaclan.placeholder;

import id.dragon.nagaclan.model.Clan;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Published on the server thread; placeholder requests may read from any thread. */
public final class ClanPlaceholderSnapshot {
    private volatile Map<UUID, Values> players = Map.of();
    public void refresh(Collection<Clan> clans) {
        Map<UUID, Values> next = new HashMap<>();
        for (Clan clan : clans) {
            clan.members().forEach((id, role) -> next.put(id, new Values(clan.name(), clan.settings().prefix(),
                    Integer.toString(clan.level()), role.name(), Long.toString(clan.bank()))));
        }
        players = Map.copyOf(next);
    }
    public String value(UUID player, String token) {
        if (token == null) return null;
        Values value = player == null ? null : players.get(player);
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "name" -> value == null ? "" : value.name();
            case "prefix" -> value == null ? "" : value.prefix();
            case "level" -> value == null ? "0" : value.level();
            case "role" -> value == null ? "" : value.role();
            case "bank" -> value == null ? "0" : value.bank();
            default -> null;
        };
    }
    public void clear() { players = Map.of(); }
    private record Values(String name, String prefix, String level, String role, String bank) {}
}
