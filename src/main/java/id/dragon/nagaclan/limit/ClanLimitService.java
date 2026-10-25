package id.dragon.nagaclan.limit;

import id.dragon.nagaclan.config.ClanLimitConfig;
import id.dragon.nagaclan.model.Clan;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.ToLongFunction;

/** Resolves and briefly caches limits derived exclusively from a clan leader. */
public final class ClanLimitService {
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private volatile ClanLimitConfig config;
    private final PermissionLookup permissionLookup;
    private final Clock clock;
    private final Duration ttl;
    private volatile Set<String> candidates;
    private volatile long generation;
    private final ConcurrentMap<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    public ClanLimitService(ClanLimitConfig config, PermissionLookup permissionLookup) {
        this(config, permissionLookup, Clock.systemUTC(), DEFAULT_TTL);
    }

    ClanLimitService(
            ClanLimitConfig config,
            PermissionLookup permissionLookup,
            Clock clock,
            Duration ttl) {
        this.config = Objects.requireNonNull(config, "config");
        this.permissionLookup = Objects.requireNonNull(permissionLookup, "permissionLookup");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.candidates = buildCandidates(config);
    }

    public CompletableFuture<ClanLimits> resolve(Clan clan) {
        Objects.requireNonNull(clan, "clan");
        UUID leaderId = clan.leaderId();
        Instant now = clock.instant();
        CacheEntry entry = cache.compute(leaderId, (ignored, cached) -> {
            if (cached != null && cached.isReusableAt(now)) {
                return cached;
            }
            return createEntry(leaderId);
        });

        entry.result().whenComplete((ignored, failure) -> {
            if (failure != null) {
                cache.remove(leaderId, entry);
            }
        });
        return entry.result();
    }

    private CacheEntry createEntry(UUID leaderId) {
        CacheEntry entry = new CacheEntry(resolveUncached(leaderId));
        entry.result().whenComplete((ignored, failure) -> {
            if (failure == null) {
                entry.recordSuccess(clock.instant().plus(ttl));
            } else {
                entry.recordFailure();
            }
        });
        return entry;
    }

    public void invalidate(UUID leaderId) {
        cache.remove(Objects.requireNonNull(leaderId, "leaderId"));
    }

    public void invalidateAll() {
        cache.clear();
    }

    /** Called on the server thread after the candidate config has been fully validated. */
    public void reconfigure(ClanLimitConfig candidate) {
        Set<String> newCandidates = buildCandidates(Objects.requireNonNull(candidate));
        config = candidate;
        candidates = newCandidates;
        generation++;
        invalidateAll();
    }

    public long generation() { return generation; }

    private CompletableFuture<ClanLimits> resolveUncached(UUID leaderId) {
        ClanLimitConfig capturedConfig = config;
        long capturedGeneration = generation;
        CompletableFuture<Set<String>> granted;
        try {
            granted = Objects.requireNonNull(
                    permissionLookup.granted(leaderId, candidates),
                    "permissionLookup returned null");
        } catch (Throwable failure) {
            return CompletableFuture.failedFuture(failure);
        }

        return granted.thenApply(nodes -> {
            if (capturedGeneration != generation) throw new java.util.concurrent.CancellationException("Leader limits reloaded");
            Set<String> safeNodes = Set.copyOf(Objects.requireNonNull(nodes, "granted permissions"));
            Selection homes = select(capturedConfig, safeNodes, capturedConfig.homesPrefix(), ClanLimitConfig.Tier::homes);
            Selection bank = select(capturedConfig, safeNodes, capturedConfig.bankPrefix(), ClanLimitConfig.Tier::bank);
            return new ClanLimits(homes.value(), bank.value(), homes.tier(), bank.tier());
        });
    }

    private Selection select(
            ClanLimitConfig config,
            Set<String> granted,
            String prefix,
            ToLongFunction<ClanLimitConfig.Tier> valueOf) {
        Map.Entry<String, ClanLimitConfig.Tier> fallback = Map.entry(
                "default", config.tiers().get("default"));
        Map.Entry<String, ClanLimitConfig.Tier> selected = null;
        long highest = Long.MIN_VALUE;
        for (Map.Entry<String, ClanLimitConfig.Tier> tier : config.tiers().entrySet()) {
            if (!granted.contains(prefix + tier.getKey())) {
                continue;
            }
            long value = valueOf.applyAsLong(tier.getValue());
            if (selected == null || value > highest) {
                selected = tier;
                highest = value;
            }
        }

        if (selected == null) {
            selected = fallback;
            highest = valueOf.applyAsLong(fallback.getValue());
        }
        return new Selection(selected.getKey(), highest);
    }

    private static Set<String> buildCandidates(ClanLimitConfig config) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (String tier : config.tiers().keySet()) {
            candidates.add(config.homesPrefix() + tier);
            candidates.add(config.bankPrefix() + tier);
        }
        return Collections.unmodifiableSet(candidates);
    }

    private static final class CacheEntry {
        private final CompletableFuture<ClanLimits> result;
        private volatile boolean completionRecorded;
        private Instant expiresAt;

        private CacheEntry(CompletableFuture<ClanLimits> result) {
            this.result = result;
        }

        private CompletableFuture<ClanLimits> result() {
            return result;
        }

        private boolean isReusableAt(Instant instant) {
            if (!completionRecorded) {
                return true;
            }
            return expiresAt != null && instant.isBefore(expiresAt);
        }

        private void recordSuccess(Instant expiresAt) {
            this.expiresAt = expiresAt;
            this.completionRecorded = true;
        }

        private void recordFailure() {
            this.completionRecorded = true;
        }
    }

    private record Selection(String tier, long value) {
    }
}
