package id.dragon.nagaclan.gui;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.service.ClanBankService;
import id.dragon.nagaclan.text.ClanColors;
import id.dragon.nagaclan.text.SmallCaps;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public final class ClanMenu {
    private static final String TITLE = "&#6D5DFB&lNagaClan &#4CC9F0Menu";

    private ClanMenu() {
    }

    public static void open(Player player) {
        Inventory menu = create(ClanMenuPage.MAIN, TITLE, Map.of(
                11, action(ClanMenuAction.OPEN_SETTINGS),
                13, action(ClanMenuAction.OPEN_HOMES),
                15, action(ClanMenuAction.OPEN_BANK)));
        menu.setItem(11, item(Material.REPEATER, ("&#8B7BFF&l" + Messages.text("Settings")), ("&8" + Messages.text("Pengaturan clan"))));
        menu.setItem(13, item(Material.ENDER_PEARL, "&#4CC9F0&lHome", ("&8" + Messages.text("Kelola clan home"))));
        menu.setItem(15, item(Material.GOLD_INGOT, ("&#A78BFA&l" + Messages.text("Bank")), ("&8" + Messages.text("Saldo clan"))));
        player.openInventory(menu);
    }

    public static void openSettings(Player player, Clan clan) {
        Inventory menu = create(ClanMenuPage.SETTINGS, "&#6D5DFB&lNagaClan &8» &#4CC9F0Settings", Map.of(
                13, action(ClanMenuAction.TOGGLE_FRIENDLY_FIRE),
                22, action(ClanMenuAction.OPEN_DASHBOARD)));
        boolean friendlyFire = clan.settings().friendlyFire();
        menu.setItem(13, item(friendlyFire ? Material.LIME_DYE : Material.RED_DYE,
                "&#8B7BFF&lClan PvP: " + (friendlyFire ? "&aON" : "&cOFF"),
                ("&8" + Messages.text("Klik untuk mengubah PvP antar anggota"))));
        menu.setItem(22, item(Material.ARROW, ("&#A9B8FF&l" + Messages.text("Kembali")), ("&8" + Messages.text("Kembali ke menu utama"))));
        player.openInventory(menu);
    }

    public static void openHomes(Player player, Clan clan) {
        Map<Integer, MenuAction> actions = new java.util.HashMap<>();
        int slot = 9;
        for (String name : clan.homes().keySet().stream().sorted().limit(9).toList()) {
            actions.put(slot++, new MenuAction(ClanMenuAction.HOME_TELEPORT, clan.homes().get(name), 0));
        }
        actions.put(22, action(ClanMenuAction.OPEN_DASHBOARD));
        Inventory menu = create(ClanMenuPage.HOMES, "&#6D5DFB&lNagaClan &8» &#4CC9F0Home", actions);
        slot = 9;
        for (String name : clan.homes().keySet().stream().sorted().limit(9).toList()) {
            menu.setItem(slot++, item(Material.ENDER_PEARL, ("&#4CC9F0&l" + Messages.text("Home:") + " &f") + name, ("&8" + Messages.text("Klik untuk teleport"))));
        }
        if (actions.size() == 1) {
            menu.setItem(13, item(Material.BARRIER, ("&#FF5C8A&l" + Messages.text("Belum ada home")), "&8Gunakan /clan sethome <nama>"));
        }
        menu.setItem(22, item(Material.ARROW, ("&#A9B8FF&l" + Messages.text("Kembali")), ("&8" + Messages.text("Kembali ke menu utama"))));
        player.openInventory(menu);
    }

    public static void openBank(Player player, Clan clan, ClanBankService bank) {
        Inventory menu = create(ClanMenuPage.BANK, "&#6D5DFB&lNagaClan &8» &#A78BFABank", Map.of(
                11, action(ClanMenuAction.DEPOSIT),
                13, action(ClanMenuAction.BANK_SETTINGS),
                15, action(ClanMenuAction.WITHDRAW),
                22, action(ClanMenuAction.OPEN_DASHBOARD)));
        String balance = bank.format(clan.bank());
        menu.setItem(11, item(Material.EMERALD, ("&#4CC9F0&l" + Messages.text("Deposit")), ("&8" + Messages.text("Masukkan saldo ke bank clan"))));
        menu.setItem(13, item(Material.COMPARATOR, ("&#A78BFA&l" + Messages.text("Akses Bank")), ("&8" + Messages.text("Atur akses deposit dan withdraw member"))));
        menu.setItem(15, item(Material.GOLD_INGOT, ("&#8B7BFF&l" + Messages.text("Withdraw")), ("&8" + Messages.text("Saldo clan:") + " &f") + balance));
        menu.setItem(22, item(Material.ARROW, ("&#A9B8FF&l" + Messages.text("Kembali")), ("&8" + Messages.text("Kembali ke menu utama"))));
        player.openInventory(menu);
    }

    public static void openAmountPicker(Player player, ClanMenuPage page, long maximum, ClanBankService bank) {
        String title = page == ClanMenuPage.DEPOSIT ? ("&#4CC9F0&l" + Messages.text("Deposit")) : ("&#8B7BFF&l" + Messages.text("Withdraw"));
        ClanMenuAction amountAction = page == ClanMenuPage.DEPOSIT ? ClanMenuAction.DEPOSIT_AMOUNT : ClanMenuAction.WITHDRAW_AMOUNT;
        Inventory menu = create(page, "&#6D5DFB&lNagaClan &8» " + title, Map.of(
                10, new MenuAction(amountAction, "10", 0),
                11, new MenuAction(amountAction, "11", 0),
                12, new MenuAction(amountAction, "12", 0),
                13, new MenuAction(amountAction, "13", 0),
                22, action(ClanMenuAction.OPEN_BANK)));
        menu.setItem(10, amountItem(1_000, maximum, bank));
        menu.setItem(11, amountItem(10_000, maximum, bank));
        menu.setItem(12, amountItem(100_000, maximum, bank));
        menu.setItem(13, item(Material.NETHER_STAR, "&#A78BFA&lSemua", ("&8" + Messages.text("Jumlah:") + " &f") + bank.format(maximum)));
        menu.setItem(22, item(Material.ARROW, ("&#A9B8FF&l" + Messages.text("Kembali")), ("&8" + Messages.text("Kembali ke bank"))));
        player.openInventory(menu);
    }

    public static void openBankSettings(Player player, Clan clan) {
        Inventory menu = create(ClanMenuPage.BANK_SETTINGS, "&#6D5DFB&lNagaClan &8» &#A78BFAAkses Bank", Map.of(
                11, action(ClanMenuAction.TOGGLE_MEMBER_DEPOSIT),
                15, action(ClanMenuAction.TOGGLE_MEMBER_WITHDRAW),
                22, action(ClanMenuAction.OPEN_BANK)));
        menu.setItem(11, item(clan.settings().memberDepositAllowed() ? Material.LIME_DYE : Material.RED_DYE,
                "&#4CC9F0&lMember Deposit: " + (clan.settings().memberDepositAllowed() ? "&aON" : "&cOFF"),
                ("&8" + Messages.text("Klik untuk mengubah akses member"))));
        menu.setItem(15, item(clan.settings().memberWithdrawAllowed() ? Material.LIME_DYE : Material.RED_DYE,
                "&#8B7BFF&lMember Withdraw: " + (clan.settings().memberWithdrawAllowed() ? "&aON" : "&cOFF"),
                ("&8" + Messages.text("Klik untuk mengubah akses member"))));
        menu.setItem(22, item(Material.ARROW, ("&#A9B8FF&l" + Messages.text("Kembali")), ("&8" + Messages.text("Kembali ke bank"))));
        player.openInventory(menu);
    }

    private static ItemStack amountItem(long amount, long maximum, ClanBankService bank) {
        long resolved = Math.min(amount, maximum);
        return item(Material.SUNFLOWER, "&#4CC9F0&l" + bank.format(resolved), ("&8" + Messages.text("Klik untuk memilih nominal")));
    }

    private static Inventory create(ClanMenuPage page, String title, Map<Integer, MenuAction> actions) {
        ClanMenuHolder holder = new ClanMenuHolder(page, actions);
        Inventory menu = Bukkit.createInventory(holder, 27, ClanColors.colorize(Messages.style(title)));
        holder.setInventory(menu);
        return menu;
    }

    private static MenuAction action(ClanMenuAction type) {
        return new MenuAction(type, "", 0);
    }

    private static ItemStack item(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ClanColors.colorize(Messages.style(name)));
        meta.setLore(List.of(ClanColors.colorize(Messages.style(lore))));
        stack.setItemMeta(meta);
        return stack;
    }
}
