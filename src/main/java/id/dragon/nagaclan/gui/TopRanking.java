package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.model.Clan;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class TopRanking {
    private TopRanking() {}

    public static List<Clan> rank(Collection<Clan> clans) {
        return clans.stream().filter(clan -> clan.settings().visibleOnTop())
                .sorted(Comparator.comparingInt(Clan::level).reversed()
                        .thenComparing(Comparator.comparingLong(Clan::experience).reversed())
                        .thenComparing(Comparator.comparingLong(Clan::bank).reversed())
                        .thenComparing(Clan::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Clan::id))
                .toList();
    }
}
