package id.dragon.nagaclan.gui;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import org.bukkit.inventory.Inventory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reserves configured navigation slots before placing list entries. */
public final class MenuListNavigation {
    private final MenuConfig config;
    private final List<Integer> slots;
    public MenuListNavigation(MenuConfig config, int size) {
        this.config = config;
        var reserved = java.util.Set.of(config.button("navigation.previous").slot(),
                config.button("navigation.page").slot(), config.button("navigation.next").slot());
        List<Integer> available = new ArrayList<>(new MenuListLayout(size).contentSlots());
        available.removeIf(reserved::contains);
        if (available.isEmpty()) for (int slot = 0; slot < size; slot++) if (!reserved.contains(slot)) available.add(slot);
        slots = List.copyOf(available);
    }
    public List<Integer> slots() { return slots; }
    public Map<Integer, MenuAction> actions(ClanMenuAction destination, int page, int pages) {
        Map<Integer, MenuAction> actions = new HashMap<>();
        if (page > 0) actions.put(config.button("navigation.previous").slot(), new MenuAction(destination, "", page - 1));
        if (page + 1 < pages) actions.put(config.button("navigation.next").slot(), new MenuAction(destination, "", page + 1));
        actions.put(config.button("navigation.page").slot(), new MenuAction(ClanMenuAction.OPEN_DASHBOARD, "", 0));
        return actions;
    }
    public void render(Inventory menu, ClanMenuRenderer renderer, int page, int pages) {
        put(menu, renderer, "navigation.page", ("&#A9B8FF" + Messages.text("Kembali")), List.of("&7" + (page + 1) + " / " + pages));
        if (page > 0) put(menu, renderer, "navigation.previous", ("&#4CC9F0" + Messages.text("Sebelumnya")), List.of());
        if (page + 1 < pages) put(menu, renderer, "navigation.next", ("&#4CC9F0" + Messages.text("Berikutnya")), List.of());
    }
    private void put(Inventory menu, ClanMenuRenderer renderer, String key, String label, List<String> lore) {
        var button = config.button(key);
        menu.setItem(button.slot(), renderer.item(button.material(), label, lore));
    }
}
