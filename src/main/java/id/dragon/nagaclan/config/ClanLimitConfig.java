package id.dragon.nagaclan.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/** Immutable leader-tier limits parsed from the plugin configuration. */
public record ClanLimitConfig(String homesPrefix, String bankPrefix, Map<String, Tier> tiers) {
    public static final String DEFAULT_HOMES_PREFIX = "nagaclans.sethome.multiple.";
    public static final String DEFAULT_BANK_PREFIX = "nagaclans.maxbank.multiple.";

    public ClanLimitConfig {
        homesPrefix = requirePrefix(homesPrefix, "homesPrefix");
        bankPrefix = requirePrefix(bankPrefix, "bankPrefix");
        Objects.requireNonNull(tiers, "tiers");
        LinkedHashMap<String, Tier> normalized = new LinkedHashMap<>();
        tiers.forEach((name, tier) -> normalized.put(normalizeTierName(name), Objects.requireNonNull(tier, "tier")));
        if (!normalized.containsKey("default")) {
            throw new IllegalArgumentException("group-limits.tiers.default is required");
        }
        tiers = Collections.unmodifiableMap(new LinkedHashMap<>(normalized));
    }

    public static ClanLimitConfig from(ConfigurationSection root) {
        Objects.requireNonNull(root, "root");
        String homesPrefix = root.getString("group-limits.permission-prefix.homes", DEFAULT_HOMES_PREFIX);
        String bankPrefix = root.getString("group-limits.permission-prefix.bank", DEFAULT_BANK_PREFIX);
        ConfigurationSection tierSection = root.getConfigurationSection("group-limits.tiers");
        if (tierSection == null) {
            throw new IllegalArgumentException("group-limits.tiers is required");
        }

        LinkedHashMap<String, Tier> tiers = new LinkedHashMap<>();
        for (String name : tierSection.getKeys(false)) {
            String path = name + ".";
            tiers.put(name.toLowerCase(Locale.ROOT), new Tier(
                    readLimit(tierSection, path + "homes"),
                    readLimit(tierSection, path + "bank")));
        }
        return new ClanLimitConfig(homesPrefix, bankPrefix, tiers);
    }

    private static long readLimit(ConfigurationSection section, String path) {
        Object value = section.get(path);
        try {
            if (!(value instanceof Number) && !(value instanceof String)) throw new NumberFormatException();
            long parsed = new java.math.BigDecimal(value.toString()).longValueExact();
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException("group-limits.tiers." + path + " harus bilangan bulat non-negatif", invalid);
        }
    }

    private static String requirePrefix(String prefix, String name) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return prefix;
    }

    private static String normalizeTierName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tier names must not be blank");
        }
        return name.toLowerCase(Locale.ROOT);
    }

    public record Tier(long homes, long bank) {
        public Tier {
            if (homes < 0L || bank < 0L) {
                throw new IllegalArgumentException("Tier limits cannot be negative");
            }
        }
    }
}
