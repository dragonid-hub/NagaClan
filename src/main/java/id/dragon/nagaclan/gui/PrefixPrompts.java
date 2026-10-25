package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.model.Clan;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe handoff only; clan data is accessed exclusively on the server thread. */
final class PrefixPrompts {
    private final ConcurrentHashMap<UUID, Prompt> pending = new ConcurrentHashMap<>();

    void begin(UUID player, Clan clan, Instant now) {
        pending.put(player, new Prompt(clan, now.plusSeconds(60)));
    }

    Optional<Clan> take(UUID player, Instant now) {
        Prompt prompt = pending.remove(player);
        return prompt == null || !now.isBefore(prompt.expires()) ? Optional.empty() : Optional.of(prompt.clan());
    }

    void remove(UUID player) { pending.remove(player); }
    void clear() { pending.clear(); }
    void expire(Instant now) { pending.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expires())); }

    private record Prompt(Clan clan, Instant expires) {}
}
