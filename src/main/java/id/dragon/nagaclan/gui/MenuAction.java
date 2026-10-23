package id.dragon.nagaclan.gui;

import java.util.Objects;

/** Immutable routing data attached to an inventory slot. */
public record MenuAction(ClanMenuAction type, String payload, int page) {
    public MenuAction {
        type = Objects.requireNonNull(type, "type");
        payload = Objects.requireNonNull(payload, "payload");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
    }
}
