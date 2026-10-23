package id.dragon.nagaclan.gui;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Compatibility lookup for explicit slot bindings; it performs no page-based inference. */
public final class ClanMenuActionResolver {
    private ClanMenuActionResolver() {
    }

    public static Optional<MenuAction> actionFor(Map<Integer, MenuAction> actions, int slot) {
        return Optional.ofNullable(Objects.requireNonNull(actions, "actions").get(slot));
    }
}
