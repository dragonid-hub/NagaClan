package id.dragon.nagaclan.service;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.model.*;
import id.dragon.nagaclan.storage.ClanStorage;
import java.io.IOException;
import java.time.*;
import java.util.*;

public final class ClanService {
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<UUID, String> playerClans = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();
    private final Map<UUID, Instant> disbandConfirmations = new HashMap<>();
    private final ClanStorage storage;
    private final java.util.function.Supplier<Set<String>> allowedIcons;
    private static final java.util.regex.Pattern COLORS = java.util.regex.Pattern.compile(
            "(?i)[&§]x(?:[&§][0-9a-f]){6}|&#[0-9a-f]{6}|<#[0-9a-f]{6}>|#[0-9a-f]{6}|[&§][0-9a-fk-or]");

    public ClanService() { this(clan -> { }); }
    public ClanService(ClanStorage storage) {
        this(storage, () -> Set.of("AMETHYST_SHARD", "DIAMOND", "NETHER_STAR", "DRAGON_HEAD", "SHIELD"));
    }
    public ClanService(ClanStorage storage, java.util.function.Supplier<Set<String>> allowedIcons) {
        this.storage = Objects.requireNonNull(storage);
        this.allowedIcons = Objects.requireNonNull(allowedIcons);
    }

    public Clan create(UUID leader, String name) {
        if (playerClans.containsKey(leader)) throw new IllegalStateException(Messages.text("Sudah memiliki clan"));
        String id = normalize(name);
        if (clans.containsKey(id)) throw new IllegalArgumentException(Messages.text("Nama clan sudah digunakan"));
        Clan clan = new Clan(id, name.trim(), leader);
        clans.put(id, clan); playerClans.put(leader, id);
        try { persist(clan); } catch (RuntimeException exception) { clans.remove(id); playerClans.remove(leader); throw exception; }
        return clan;
    }
    public Optional<Clan> clanOf(UUID player) { return Optional.ofNullable(playerClans.get(player)).map(clans::get); }
    public Optional<Clan> find(String name) { return Optional.ofNullable(clans.get(normalize(name))); }
    public Collection<Clan> allClans() { return List.copyOf(clans.values()); }
    public void restoreAll(Collection<Clan> restoredClans) {
        clans.clear();
        playerClans.clear();
        for (Clan clan : restoredClans) {
            clans.put(clan.id(), clan);
            clan.members().keySet().forEach(member -> playerClans.put(member, clan.id()));
        }
    }
    public void invite(UUID actor, UUID target, String clanName, Duration duration) {
        Clan clan = requireAccess(actor, ClanAccess.INVITE);
        if (playerClans.containsKey(target)) throw new IllegalStateException(Messages.text("Pemain sudah memiliki clan"));
        invitations.put(target, new Invitation(clan.id(), Instant.now().plus(duration)));
    }
    public Clan accept(UUID target) {
        Invitation invite = Optional.ofNullable(invitations.remove(target)).filter(i -> !i.expired()).orElseThrow(() -> new IllegalStateException(Messages.text("Undangan tidak tersedia")));
        if (playerClans.containsKey(target)) throw new IllegalStateException(Messages.text("Sudah memiliki clan"));
        Clan clan = Optional.ofNullable(clans.get(invite.clanId())).orElseThrow(() -> new IllegalStateException(Messages.text("Clan sudah tidak ada")));
        clan.addMember(target, ClanRole.MEMBER); playerClans.put(target, clan.id());
        try { persist(clan); } catch (RuntimeException exception) { clan.removeMember(target); playerClans.remove(target); invitations.put(target, invite); throw exception; }
        return clan;
    }
    public void deny(UUID target) { invitations.remove(target); }
    public void setHome(UUID actor, String name, String serializedLocation, long limit) { Clan clan=requireAccess(actor, ClanAccess.SET_HOME); String key=normalizeHome(name); if(!clan.homes().containsKey(key) && clan.homes().size()>=limit) throw new IllegalStateException(Messages.text("Batas home clan tercapai")); String previous=clan.homes().put(key,serializedLocation); try { persist(clan); } catch (RuntimeException exception) { if(previous==null) clan.homes().remove(key); else clan.homes().put(key,previous); throw exception; } }
    public Optional<String> home(UUID player,String name) { return Optional.ofNullable(requireAccess(player, ClanAccess.HOME).homes().get(normalizeHome(name))); }
    public void deleteHome(UUID actor, String name) {
        Clan clan = requireAccess(actor, ClanAccess.DELETE_HOME);
        String key = normalizeHome(name);
        String previous = clan.homes().remove(key);
        if (previous == null) throw new IllegalStateException(Messages.text("Home tidak ditemukan"));
        saveSetting(clan, () -> clan.homes().put(key, previous));
    }
    public Clan requireAccess(UUID actor, ClanAccess action) {
        Clan clan = requireClan(actor);
        if (!clan.settings().allows(clan.roleOf(actor), action)) throw new IllegalStateException(Messages.text("Role tidak cukup"));
        return clan;
    }
    public void setAccess(UUID actor, ClanAccess action, ClanRole role) {
        Clan clan = requireLeader(actor);
        ClanRole previous = clan.settings().access(action);
        clan.settings().setAccess(action, role);
        saveSetting(clan, () -> clan.settings().setAccess(action, previous));
    }
    public void requestAlliance(UUID actor,String clanName) { Clan clan=requireAccess(actor, ClanAccess.ALLIANCE); Clan other=find(clanName).orElseThrow(()->new IllegalStateException(Messages.text("Clan tidak ditemukan"))); if(other.id().equals(clan.id())) throw new IllegalStateException(Messages.text("Tidak dapat beraliansi sendiri")); if(clan.alliances().add(other.id())) { try { persist(clan); } catch (RuntimeException exception) { clan.alliances().remove(other.id()); throw exception; } } }
    public void leave(UUID player) { Clan clan=requireClan(player); ClanRole previous=clan.roleOf(player); if(previous==ClanRole.LEADER) throw new IllegalStateException(Messages.text("Leader harus transfer atau disband clan")); clan.removeMember(player); playerClans.remove(player); try { persist(clan); } catch (RuntimeException exception) { clan.addMember(player,previous); playerClans.put(player,clan.id()); throw exception; } }
    public void kick(UUID actor, UUID target) { Clan clan=requireAccess(actor, ClanAccess.KICK); ClanRole actorRole=clan.roleOf(actor); ClanRole previous=clan.roleOf(target); if(actor.equals(target) || previous==null || actorRole.ordinal()>=previous.ordinal()) throw new IllegalStateException(Messages.text("Target tidak valid")); clan.removeMember(target); playerClans.remove(target); try { persist(clan); } catch (RuntimeException exception) { clan.addMember(target,previous); playerClans.put(target,clan.id()); throw exception; } }
    public void setRole(UUID actor, UUID target, ClanRole role) { Clan clan=requireClan(actor); ClanRole actorRole=clan.roleOf(actor); ClanRole previous=clan.roleOf(target); if(actor.equals(target) || previous==null || actorRole.ordinal()>=previous.ordinal()) throw new IllegalStateException(Messages.text("Target bukan anggota clan")); if(role==ClanRole.LEADER) { if(actorRole!=ClanRole.LEADER) throw new IllegalStateException(Messages.text("Hanya leader dapat memindahkan leadership")); clan.setRole(actor,ClanRole.ADMIN); clan.setRole(target,ClanRole.LEADER); try { persist(clan); } catch (RuntimeException exception) { clan.setRole(actor,ClanRole.LEADER); clan.setRole(target,previous); throw exception; } return; } requireAccess(actor, role == ClanRole.ADMIN ? ClanAccess.SET_ADMIN : ClanAccess.SET_BUILDER); if (actorRole.ordinal() >= role.ordinal()) throw new IllegalStateException(Messages.text("Role tidak cukup")); clan.setRole(target,role); try { persist(clan); } catch (RuntimeException exception) { clan.setRole(target,previous); throw exception; } }
    public void setFriendlyFire(UUID actor, boolean enabled) { Clan clan=requireAccess(actor, ClanAccess.PVP); boolean previous=clan.settings().friendlyFire(); clan.settings().setFriendlyFire(enabled); try { persist(clan); } catch (RuntimeException exception) { clan.settings().setFriendlyFire(previous); throw exception; } }
    public void save(Clan clan) { persist(clan); }
    public void setVisibleOnTop(UUID actor, boolean visible) {
        Clan clan = requireAccess(actor, ClanAccess.TOP_VISIBILITY);
        boolean previous = clan.settings().visibleOnTop();
        clan.settings().setVisibleOnTop(visible);
        saveSetting(clan, () -> clan.settings().setVisibleOnTop(previous));
    }

    public void setPrefix(UUID actor, String prefix) {
        Clan clan = requireAccess(actor, ClanAccess.PREFIX);
        if (prefix == null || prefix.length() > 256 || prefix.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(Messages.text("Prefix harus 1-12 karakter terlihat tanpa baris baru"));
        }
        String value = prefix.strip();
        String visible = COLORS.matcher(value).replaceAll("").strip();
        int length = visible.codePointCount(0, visible.length());
        if (length < 1 || length > 12) throw new IllegalArgumentException(Messages.text("Prefix harus 1-12 karakter terlihat"));
        String previous = clan.settings().prefix();
        clan.settings().setPrefix(value);
        saveSetting(clan, () -> clan.settings().setPrefix(previous));
    }

    public void setIcon(UUID actor, String icon) {
        Clan clan = requireAccess(actor, ClanAccess.ICON);
        String value = icon == null ? "" : icon.strip().toUpperCase(Locale.ROOT);
        if (!allowedIcons.get().contains(value)) throw new IllegalArgumentException(Messages.text("Icon tidak diizinkan di config"));
        String previous = clan.settings().icon();
        clan.settings().setIcon(value);
        saveSetting(clan, () -> clan.settings().setIcon(previous));
    }

    private Clan requireLeader(UUID actor) {
        Clan clan = requireClan(actor);
        requireRole(clan, actor, ClanRole.LEADER);
        return clan;
    }

    private void saveSetting(Clan clan, Runnable rollback) {
        try { persist(clan); }
        catch (RuntimeException failure) { rollback.run(); throw failure; }
    }
    public void saveAll() { clans.values().forEach(this::persist); }
    public boolean confirmDisband(UUID leader) { Clan clan=requireLeader(leader); Instant now=Instant.now(); Instant previous=disbandConfirmations.put(leader,now); if(previous==null || !now.isBefore(previous.plusSeconds(30))) return false; disband(leader); return true; }
    /** Called only after command or GUI confirmation; authority is rechecked at execution. */
    public void disband(UUID leader) {
        Clan clan = requireLeader(leader);
        delete(clan.id());
        clans.remove(clan.id());
        clan.members().keySet().forEach(playerClans::remove);
        disbandConfirmations.remove(leader);
    }
    private Clan requireClan(UUID player) { return clanOf(player).orElseThrow(() -> new IllegalStateException(Messages.text("Tidak memiliki clan"))); }
    private void requireRole(Clan clan, UUID player, ClanRole minimum) { ClanRole role=clan.roleOf(player); if(role==null || role.ordinal()>minimum.ordinal()) throw new IllegalStateException(Messages.text("Role tidak cukup")); }
    private void persist(Clan clan) { try { storage.save(clan); } catch (IOException exception) { throw new IllegalStateException(Messages.text("Gagal menyimpan data clan"), exception); } }
    private void delete(String clanId) { try { storage.delete(clanId); } catch (IOException exception) { throw new IllegalStateException(Messages.text("Gagal menghapus data clan"), exception); } }
    private String normalize(String name) { if(name==null || !name.matches("[A-Za-z0-9_]{3,16}")) throw new IllegalArgumentException(Messages.text("Nama clan harus 3-16 karakter alfanumerik")); return name.toLowerCase(Locale.ROOT); }
    private String normalizeHome(String name) { if(name==null || !name.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException(Messages.text("Nama home tidak valid")); return name.toLowerCase(Locale.ROOT); }
}
