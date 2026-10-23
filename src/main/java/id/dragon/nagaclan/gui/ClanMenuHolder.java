package id.dragon.nagaclan.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ClanMenuHolder implements InventoryHolder {
    private final ClanMenuPage page;
    private final Map<Integer, MenuAction> actions;
    private Inventory inventory;
    private id.dragon.nagaclan.model.Clan expectedClan;
    private boolean bound;

    public ClanMenuHolder(ClanMenuPage page, Map<Integer, MenuAction> actions) {
        this.page = Objects.requireNonNull(page, "page");
        this.actions = Map.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ClanMenuPage page() {
        return page;
    }

    public void bindClan(id.dragon.nagaclan.model.Clan clan) {
        if (bound) throw new IllegalStateException("Menu already bound");
        expectedClan = clan;
        bound = true;
    }

    public boolean accepts(id.dragon.nagaclan.model.Clan clan, MenuAction action) {
        return (!bound || expectedClan == clan) && actions.containsValue(action);
    }

    public Optional<MenuAction> actionAt(int slot) {
        return Optional.ofNullable(actions.get(slot));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
