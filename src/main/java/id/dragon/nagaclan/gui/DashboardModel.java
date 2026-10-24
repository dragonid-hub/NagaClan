package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/** Immutable, leader-limit-aware values shown by the dashboard. */
public record DashboardModel(String clanName, int memberCount, int level, long experience,
                             int allianceCount, String homeUsage, String bankUsage) {
    public static DashboardModel from(Clan clan, ClanLimits limits) {
        Objects.requireNonNull(clan, "clan");
        Objects.requireNonNull(limits, "limits");
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
        return new DashboardModel(
                clan.name(),
                clan.members().size(),
                clan.level(),
                clan.experience(),
                clan.alliances().size(),
                clan.homes().size() + " / " + limits.homes(),
                numbers.format(clan.bank()) + " / " + numbers.format(limits.bank()));
    }
}
