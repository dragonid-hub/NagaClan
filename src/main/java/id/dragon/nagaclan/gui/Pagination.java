package id.dragon.nagaclan.gui;

import java.util.List;
import java.util.Objects;

/** Pure helpers for safely slicing list-backed inventory pages. */
public final class Pagination {
    private Pagination() {
    }

    public static <T> List<T> slice(List<T> items, int page, int pageSize) {
        Objects.requireNonNull(items, "items");
        int pages = pageCount(items.size(), pageSize);
        if (pages == 0) {
            return List.of();
        }
        int safePage = Math.max(0, Math.min(page, pages - 1));
        int start = safePage * pageSize;
        return List.copyOf(items.subList(start, Math.min(start + pageSize, items.size())));
    }

    public static int pageCount(int items, int pageSize) {
        if (items < 0) {
            throw new IllegalArgumentException("items must not be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        return items == 0 ? 0 : (items + pageSize - 1) / pageSize;
    }
}
