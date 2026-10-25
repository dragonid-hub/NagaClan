package id.dragon.nagaclan.economy;

public record EconomyResult(boolean successful, String message) {
    public static EconomyResult success() {
        return new EconomyResult(true, "");
    }

    public static EconomyResult failure(String message) {
        return new EconomyResult(false, message == null ? "" : message);
    }
}
