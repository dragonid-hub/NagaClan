package id.dragon.nagaclan.limit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/** Uses Bukkit permissions online and an optional reflection-only LuckPerms bridge offline. */
public final class BukkitLuckPermsPermissionLookup implements PermissionLookup {
    private static final Duration WARNING_INTERVAL = Duration.ofMinutes(1);

    private final Consumer<String> warningSink;
    private final Clock clock;
    private final Duration warningInterval;
    private Instant nextWarning = Instant.MIN;

    public BukkitLuckPermsPermissionLookup() {
        this(message -> Bukkit.getLogger().warning(message));
    }

    public BukkitLuckPermsPermissionLookup(Consumer<String> warningSink) {
        this(warningSink, Clock.systemUTC(), WARNING_INTERVAL);
    }

    BukkitLuckPermsPermissionLookup(
            Consumer<String> warningSink,
            Clock clock,
            Duration warningInterval) {
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.warningInterval = Objects.requireNonNull(warningInterval, "warningInterval");
        if (warningInterval.isNegative()) {
            throw new IllegalArgumentException("warningInterval must not be negative");
        }
    }

    @Override
    public CompletableFuture<Set<String>> granted(UUID playerId, Collection<String> candidates) {
        Objects.requireNonNull(playerId, "playerId");
        Set<String> candidateSnapshot = Set.copyOf(Objects.requireNonNull(candidates, "candidates"));
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            return CompletableFuture.completedFuture(grantedOnline(player, candidateSnapshot));
        }
        return grantedOffline(playerId, candidateSnapshot);
    }

    private Set<String> grantedOnline(Player player, Set<String> candidates) {
        Map<String, String> normalizedCandidates = new LinkedHashMap<>();
        for (String candidate : candidates) {
            normalizedCandidates.put(candidate.toLowerCase(Locale.ROOT), candidate);
        }

        LinkedHashSet<String> granted = new LinkedHashSet<>();
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            if (!permission.getValue()) {
                continue;
            }
            String candidate = normalizedCandidates.get(permission.getPermission().toLowerCase(Locale.ROOT));
            if (candidate != null) {
                granted.add(candidate);
            }
        }
        return Set.copyOf(granted);
    }

    private CompletableFuture<Set<String>> grantedOffline(UUID playerId, Set<String> candidates) {
        try {
            Class<?> providerType = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerType.getMethod("get").invoke(null);
            Object userManager = apiMethod("net.luckperms.api.LuckPerms", "getUserManager")
                    .invoke(luckPerms);
            Object loadResult = apiMethod(
                    "net.luckperms.api.model.user.UserManager", "loadUser", UUID.class)
                    .invoke(userManager, playerId);
            if (!(loadResult instanceof CompletionStage<?> loadedUser)) {
                warn("LuckPerms loadUser did not return a CompletionStage", null);
                return CompletableFuture.completedFuture(Set.of());
            }

            return loadedUser
                    .thenApply(user -> grantedFromUser(user, candidates))
                    .handle((granted, failure) -> {
                        if (failure == null) {
                            return granted;
                        }
                        warn("LuckPerms could not load offline permissions", failure);
                        return Set.<String>of();
                    })
                    .toCompletableFuture();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            warn("LuckPerms is unavailable for offline permission lookup", failure);
            return CompletableFuture.completedFuture(Set.of());
        }
    }

    private Set<String> grantedFromUser(Object user, Set<String> candidates) {
        try {
            Object cachedData = apiMethod("net.luckperms.api.model.user.User", "getCachedData")
                    .invoke(user);
            Object permissionData = apiMethod(
                    "net.luckperms.api.cacheddata.CachedDataManager", "getPermissionData")
                    .invoke(cachedData);
            Method checkPermission = apiMethod(
                    "net.luckperms.api.cacheddata.CachedPermissionData",
                    "checkPermission",
                    String.class);
            LinkedHashSet<String> granted = new LinkedHashSet<>();
            for (String candidate : candidates) {
                Object tristate = checkPermission.invoke(permissionData, candidate);
                Object allowed = apiMethod("net.luckperms.api.util.Tristate", "asBoolean")
                        .invoke(tristate);
                if (Boolean.TRUE.equals(allowed)) {
                    granted.add(candidate);
                }
            }
            return Set.copyOf(granted);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            throw new CompletionException(failure);
        }
    }

    private static Method apiMethod(String typeName, String name, Class<?>... parameterTypes)
            throws ReflectiveOperationException {
        return Class.forName(typeName).getMethod(name, parameterTypes);
    }

    private synchronized void warn(String message, Throwable failure) {
        Instant now = clock.instant();
        if (now.isBefore(nextWarning)) {
            return;
        }
        nextWarning = now.plus(warningInterval);
        String detail = failure == null ? "" : " (" + rootCause(failure).getClass().getSimpleName() + ")";
        try {
            warningSink.accept(message + detail + "; using configured default clan limits.");
        } catch (RuntimeException ignored) {
            // Logging must never turn optional LuckPerms absence into a lookup failure.
        }
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable result = failure;
        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }
        return result;
    }
}
