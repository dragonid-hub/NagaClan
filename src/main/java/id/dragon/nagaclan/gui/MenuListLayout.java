package id.dragon.nagaclan.gui;

import java.util.ArrayList;
import java.util.List;

/** Compact fallback for small inventories; larger pages retain an empty outer frame. */
public record MenuListLayout(int size) {
    public MenuListLayout {
        if (size < 9 || size > 54 || size % 9 != 0) throw new IllegalArgumentException("Invalid inventory size");
    }
    public List<Integer> contentSlots() {
        List<Integer> slots = new ArrayList<>();
        if (size <= 18) {
            for (int slot = 0; slot < previous(); slot++) slots.add(slot);
        } else {
            for (int row = 1; row < size / 9 - 1; row++) {
                for (int column = 1; column < 8; column++) slots.add(row * 9 + column);
            }
        }
        return List.copyOf(slots);
    }
    public int previous() { return size == 9 ? 6 : size - 9; }
    public int back() { return size == 9 ? 7 : size - 5; }
    public int next() { return size - 1; }
}
