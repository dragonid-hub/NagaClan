package id.dragon.nagaclan.gui;

public final class BankAmountResolver {
    private BankAmountResolver() {
    }

    public static long amountFor(int slot, long maximum) {
        if (maximum <= 0L) return 0L;
        long requested = switch (slot) {
            case 10 -> 1_000L;
            case 11 -> 10_000L;
            case 12 -> 100_000L;
            case 13 -> maximum;
            default -> 0L;
        };
        return Math.min(requested, maximum);
    }
}
