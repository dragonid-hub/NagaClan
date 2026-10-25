package id.dragon.nagaclan.storage;

import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.model.ClanRole;
import id.dragon.nagaclan.model.ClanAccess;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Writes the durable clan fields; UUID membership is kept by ClanService. */
public final class YamlClanStorage implements ClanStorage {
    private final File file;
    public YamlClanStorage(File dataFolder) { this.file = new File(dataFolder, "clans.yml"); }
    public void save(Clan clan) throws IOException {
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        String path = "clans." + clan.id() + ".";
        yaml.set(path + "name", clan.name()); yaml.set(path + "level", clan.level()); yaml.set(path + "experience", clan.experience()); yaml.set(path + "bank", clan.bank());
        yaml.set(path + "members", clan.members().entrySet().stream().collect(java.util.stream.Collectors.toMap(entry -> entry.getKey().toString(), entry -> entry.getValue().name())));
        yaml.set(path + "homes", clan.homes()); yaml.set(path + "alliances", clan.alliances().stream().toList());
        yaml.set(path + "settings.friendly-fire", clan.settings().friendlyFire()); yaml.set(path + "settings.visible-on-top", clan.settings().visibleOnTop());
        yaml.set(path + "settings.member-deposit-allowed", clan.settings().memberDepositAllowed());
        yaml.set(path + "settings.member-withdraw-allowed", clan.settings().memberWithdrawAllowed());
        yaml.set(path + "settings.prefix", clan.settings().prefix()); yaml.set(path + "settings.icon", clan.settings().icon());
        for (var action : ClanAccess.values()) {
            yaml.set(path + "settings.access." + action.name(), clan.settings().access(action).name());
        }
        writeAtomically(yaml);
    }

    @Override
    public void delete(String clanId) throws IOException {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("clans." + clanId, null);
        writeAtomically(yaml);
    }

    public List<Clan> load() {
        if (!file.exists()) return List.of();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("clans");
        if (root == null) return List.of();
        List<Clan> loaded = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            ConfigurationSection members = section == null ? null : section.getConfigurationSection("members");
            if (section == null || members == null) continue;
            UUID leader = members.getKeys(false).stream()
                    .filter(key -> "LEADER".equalsIgnoreCase(members.getString(key)))
                    .map(UUID::fromString).findFirst().orElse(null);
            if (leader == null) continue;
            Clan clan = new Clan(id, section.getString("name", id), leader);
            for (String memberId : members.getKeys(false)) {
                UUID member = UUID.fromString(memberId);
                ClanRole role = ClanRole.valueOf(members.getString(memberId, "MEMBER"));
                if (!member.equals(leader)) clan.addMember(member, role);
            }
            clan.setBank(section.getLong("bank", 0L));
            clan.restoreProgress(section.getInt("level", 1), section.getLong("experience", 0L));
            ConfigurationSection homes = section.getConfigurationSection("homes");
            if (homes != null) {
                for (String homeName : homes.getKeys(false)) {
                    clan.homes().put(homeName, homes.getString(homeName, ""));
                }
            }
            clan.settings().setFriendlyFire(section.getBoolean("settings.friendly-fire", false));
            clan.settings().setMemberDepositAllowed(section.getBoolean("settings.member-deposit-allowed", true));
            clan.settings().setMemberWithdrawAllowed(section.getBoolean("settings.member-withdraw-allowed", false));
            clan.settings().setVisibleOnTop(section.getBoolean("settings.visible-on-top", true));
            clan.settings().setPrefix(section.getString("settings.prefix", ""));
            clan.settings().setIcon(section.getString("settings.icon", "AMETHYST_SHARD"));
            for (var action : ClanAccess.values()) {
                String role = section.getString("settings.access." + action.name());
                if (role != null) {
                    try { clan.settings().setAccess(action, ClanRole.valueOf(role)); }
                    catch (IllegalArgumentException invalid) { clan.settings().setAccess(action, ClanRole.LEADER); }
                }
            }
            clan.alliances().addAll(section.getStringList("alliances"));
            loaded.add(clan);
        }
        return loaded;
    }

    private void writeAtomically(YamlConfiguration yaml) throws IOException {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Tidak dapat membuat folder data NagaClan");
        }
        File temporary = File.createTempFile("clans-", ".tmp", parent);
        try {
            yaml.save(temporary);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }
}
