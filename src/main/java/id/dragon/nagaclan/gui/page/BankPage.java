package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.ClanMenuAction;
import id.dragon.nagaclan.gui.ClanMenuPage;
import id.dragon.nagaclan.gui.ClanMenuRenderer;
import id.dragon.nagaclan.gui.MenuAction;
import id.dragon.nagaclan.gui.MenuPageRenderer;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.model.ClanRole;
import id.dragon.nagaclan.service.ClanBankService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Bank controls whose displayed deposit capacity matches the executed capacity. */
public final class BankPage implements MenuPageRenderer {
    private final ClanMenuRenderer renderer;
    private final MenuConfig config;
    private final ClanBankService bank;

    public BankPage(ClanMenuRenderer renderer, MenuConfig config, ClanBankService bank) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.config = Objects.requireNonNull(config, "config");
        this.bank = Objects.requireNonNull(bank, "bank");
    }

    public static Map<Integer, MenuAction> actions(List<Long> amounts, long capacity, long balance, boolean leader, int size) {
        Slots slots = new Slots(size);
        Map<Integer, MenuAction> actions = new HashMap<>();
        for (int index = 0; index < Math.min(amounts.size(), slots.presets()); index++) {
            actions.put(slots.deposit(index), amount(ClanMenuAction.BANK_DEPOSIT, Math.min(amounts.get(index), Math.max(0, capacity))));
            actions.put(slots.withdraw(index), amount(ClanMenuAction.BANK_WITHDRAW, Math.min(amounts.get(index), Math.max(0, balance))));
        }
        actions.put(slots.depositAll(), amount(ClanMenuAction.BANK_DEPOSIT, Math.max(0, capacity)));
        actions.put(slots.withdrawAll(), amount(ClanMenuAction.BANK_WITHDRAW, Math.max(0, balance)));
        if (leader) {
            actions.put(slots.depositPolicy(), new MenuAction(ClanMenuAction.TOGGLE_MEMBER_DEPOSIT, "", 0));
            actions.put(slots.withdrawPolicy(), new MenuAction(ClanMenuAction.TOGGLE_MEMBER_WITHDRAW, "", 0));
        }
        actions.put(size - 1, new MenuAction(ClanMenuAction.OPEN_DASHBOARD, "", 0));
        return Map.copyOf(actions);
    }

    @Override public Inventory render(Player player, Clan clan, ClanLimits limits, int page) {
        long remaining = Math.max(0, limits.bank() - clan.bank());
        long capacity = Math.min(bank.playerBalance(player.getUniqueId()), remaining);
        int size = config.page("bank").size();
        Slots slots = new Slots(size);
        var actions = actions(config.bankAmounts(), capacity, clan.bank(), clan.roleOf(player.getUniqueId()) == ClanRole.LEADER, size);
        Inventory menu = renderer.create(ClanMenuPage.BANK, actions);
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.ROOT);
        menu.setItem(slots.summary(), renderer.item(Material.GOLD_INGOT, ("&#A78BFA&l" + Messages.text("Bank clan")), List.of(
                fixed(("&7" + Messages.text("Economy:") + " &f") + (bank.economyAvailable() ? Messages.text("tersedia") : Messages.text("tidak tersedia"))),
                fixed(("&7" + Messages.text("Saldo:") + " &f")) + numbers.format(clan.bank()) + " / " + numbers.format(limits.bank()),
                fixed(("&7" + Messages.text("Kapasitas deposit:") + " &f")) + numbers.format(capacity))));
        actions.forEach((slot, action) -> {
            switch (action.type()) {
                case BANK_DEPOSIT, BANK_WITHDRAW -> {
                    boolean deposit = action.type() == ClanMenuAction.BANK_DEPOSIT;
                    boolean all = slot == slots.depositAll() || slot == slots.withdrawAll();
                    String label = (deposit ? ("&#4CC9F0&l" + Messages.text("Deposit")) : ("&#A78BFA&l" + Messages.text("Withdraw"))) + (all ? (" " + Messages.text("semua")) : "");
                    menu.setItem(slot, renderer.item(deposit ? Material.EMERALD : Material.SUNFLOWER, label,
                            List.of("&f" + numbers.format(Long.parseLong(action.payload())), fixed(("&7" + Messages.text("Klik untuk transaksi"))))));
                }
                case TOGGLE_MEMBER_DEPOSIT, TOGGLE_MEMBER_WITHDRAW -> {
                    boolean deposit = action.type() == ClanMenuAction.TOGGLE_MEMBER_DEPOSIT;
                    boolean enabled = deposit ? clan.settings().memberDepositAllowed() : clan.settings().memberWithdrawAllowed();
                    menu.setItem(slot, renderer.item(enabled ? Material.LIME_DYE : Material.RED_DYE,
                            deposit ? ("&#4CC9F0" + Messages.text("Akses deposit member")) : ("&#A78BFA" + Messages.text("Akses withdraw member")),
                            List.of(fixed(enabled ? ("&#4CC9F0" + Messages.text("Diizinkan")) : ("&#FF5C8A" + Messages.text("Dilarang"))))));
                }
                case OPEN_DASHBOARD -> menu.setItem(slot, renderer.item(Material.ARROW, ("&#A9B8FF" + Messages.text("Kembali")), List.of()));
                default -> throw new IllegalStateException("Unexpected bank action");
            }
        });
        return menu;
    }

    private static MenuAction amount(ClanMenuAction type, long value) { return new MenuAction(type, Long.toString(value), 0); }
    private static String fixed(String value) { return Messages.style(value); }
    private record Slots(int size) {
        Slots { new id.dragon.nagaclan.gui.MenuListLayout(size); }
        int presets() { return size == 9 ? 1 : 3; }
        int deposit(int index) { return size >= 36 ? 20 + index : index; }
        int withdraw(int index) { return size >= 36 ? 29 + index : (size == 9 ? 1 : 4 + index); }
        int depositAll() { return size >= 36 ? 23 : (size == 9 ? 2 : 3); }
        int withdrawAll() { return size >= 36 ? 32 : (size == 9 ? 3 : 7); }
        int summary() { return size >= 36 ? 13 : (size == 9 ? 4 : 8); }
        int depositPolicy() { return size >= 36 ? 33 : (size == 9 ? 5 : 9); }
        int withdrawPolicy() { return size >= 36 ? 34 : (size == 9 ? 6 : 10); }
    }
}
