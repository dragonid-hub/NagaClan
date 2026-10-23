package id.dragon.nagaclan.gui.page;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.*;
import id.dragon.nagaclan.text.SmallCaps;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import java.util.HashMap;
import java.util.List;

public final class OnboardingPage {
    private OnboardingPage() {}
    public static Inventory render(MenuConfig config) {
        var actions = new HashMap<Integer, MenuAction>();
        int slot = 20;
        for (var type : NoClanMenuPolicy.actions().stream().sorted().toList()) {
            actions.put(slot, new MenuAction(type, "", 0));
            slot += 2;
        }
        var renderer = new ClanMenuRenderer(config);
        var menu = renderer.create(ClanMenuPage.MAIN, actions);
        actions.forEach((position, action) -> {
            String command = switch (action.type()) {
                case CREATE_HELP -> "/clan create <nama>";
                case ACCEPT_HELP -> "/clan accept";
                default -> "/clan deny";
            };
            menu.setItem(position, renderer.item(Material.PAPER, "&#4CC9F0&l" + switch (action.type()) {
                case CREATE_HELP -> Messages.text("Buat clan");
                case ACCEPT_HELP -> Messages.text("Terima undangan");
                default -> Messages.text("Tolak undangan");
            }, List.of(Messages.style(("&7" + Messages.text("Klik untuk panduan"))), "&f" + Messages.usage(command))));
        });
        return menu;
    }
}
