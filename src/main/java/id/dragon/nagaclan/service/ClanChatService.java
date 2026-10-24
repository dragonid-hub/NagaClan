package id.dragon.nagaclan.service;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.model.Clan;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Only mode lookup is async-safe. Clan state and recipients are resolved on the server thread. */
public final class ClanChatService {
    private final ClanService clans;
    private final ConcurrentHashMap<UUID, Clan> modes = new ConcurrentHashMap<>();
    public ClanChatService(ClanService clans) { this.clans = Objects.requireNonNull(clans); }
    public boolean toggle(UUID actor) {
        Clan clan = clans.clanOf(actor).orElseThrow(() -> new IllegalStateException(Messages.text("Tidak memiliki clan")));
        if (modes.remove(actor, clan)) return false;
        clans.requireAccess(actor, id.dragon.nagaclan.model.ClanAccess.CHAT);
        modes.put(actor, clan);
        return true;
    }
    public Optional<Clan> mode(UUID actor) { return Optional.ofNullable(modes.get(actor)); }
    public Message message(UUID actor, Clan expected, String input) {
        Clan current = clans.clanOf(actor).orElse(null);
        if (current == null || current != expected) {
            forget(actor);
            throw new IllegalStateException(Messages.text("Clan sudah berubah. Aktifkan ulang clan chat."));
        }
        if (input == null || input.codePoints().anyMatch(Character::isISOControl)) throw new IllegalArgumentException(Messages.text("Pesan tidak valid"));
        clans.requireAccess(actor, id.dragon.nagaclan.model.ClanAccess.CHAT);
        String body = input.strip();
        int count = body.codePointCount(0, body.length());
        if (count < 1 || count > 256) throw new IllegalArgumentException(Messages.text("Pesan harus 1-256 karakter"));
        return new Message(current.name(), body, Set.copyOf(current.members().keySet()));
    }
    public void forget(UUID actor) { modes.remove(actor); }
    public void clear() { modes.clear(); }
    public record Message(String clanName, String body, Set<UUID> recipients) {
        public Message { recipients = Set.copyOf(recipients); }
    }
}
