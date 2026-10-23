package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.model.Clan;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Main-thread, one-use approvals scoped to an exact clan instance. */
public final class MenuConfirmations {
    private final Map<UUID, Pending> pending = new HashMap<>();
    public void begin(UUID actor, Clan clan, MenuAction action, Instant now) {
        pending.values().removeIf(value -> !now.isBefore(value.expires()));
        pending.put(actor, new Pending(clan, action, now.plusSeconds(30)));
    }
    public Optional<MenuAction> take(UUID actor, Clan clan, Instant now) {
        Pending value = pending.remove(actor);
        return value == null || value.clan() != clan || !now.isBefore(value.expires())
                ? Optional.empty() : Optional.of(value.action());
    }
    public void remove(UUID actor) { pending.remove(actor); }
    public void clear() { pending.clear(); }
    private record Pending(Clan clan, MenuAction action, Instant expires) {}
}
