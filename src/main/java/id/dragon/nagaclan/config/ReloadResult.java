package id.dragon.nagaclan.config;

/** Local reload response, independent of NagaCore. */
public record ReloadResult(boolean successful, String message) {
    public static ReloadResult success(String message) { return new ReloadResult(true, message); }
    public static ReloadResult failure(String message) { return new ReloadResult(false, message); }
}
