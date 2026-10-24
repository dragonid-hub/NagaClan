package id.dragon.nagaclan.config;

import id.dragon.nagaclan.limit.ClanLimitService;
import id.dragon.nagaclan.text.Messages;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/** Standalone reload transaction used by /nc and /nagaclan. */
public final class NagaClanReloadService {
    private final NagaClanConfig config;
    private final ClanLimitService limits;
    private final BooleanSupplier serverThread;
    private final Runnable refreshMenus;
    public NagaClanReloadService(NagaClanConfig config, ClanLimitService limits, BooleanSupplier serverThread, Runnable refreshMenus) {
        this.config = Objects.requireNonNull(config);
        this.limits = Objects.requireNonNull(limits);
        this.serverThread = Objects.requireNonNull(serverThread);
        this.refreshMenus = Objects.requireNonNull(refreshMenus);
    }
    public CompletableFuture<ReloadResult> reload() {
        if (!serverThread.getAsBoolean()) return CompletableFuture.completedFuture(ReloadResult.failure(Messages.get("reload.thread")));
        try {
            config.reload();
        } catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(ReloadResult.failure(Messages.get("reload.invalid") + failure.getMessage()));
        }
        limits.reconfigure(config.limits());
        try { refreshMenus.run(); }
        catch (RuntimeException failure) {
            return CompletableFuture.completedFuture(ReloadResult.success(Messages.get("reload.menus")));
        }
        return CompletableFuture.completedFuture(ReloadResult.success(Messages.get("reload.success")));
    }
}
