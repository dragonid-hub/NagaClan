package id.dragon.nagaclan.limit;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Resolves which candidate permission nodes are granted to one player. */
@FunctionalInterface
public interface PermissionLookup {
    CompletableFuture<Set<String>> granted(UUID playerId, Collection<String> candidates);
}
