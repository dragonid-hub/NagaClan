package id.dragon.nagaclan.limit;

/** The clan limits selected independently from its leader's permission tiers. */
public record ClanLimits(long homes, long bank, String homeTier, String bankTier) {
}
