package id.dragon.nagaclan.command;

import id.dragon.nagaclan.gui.ClanMenuAction;
import java.util.Locale;
import java.util.function.Predicate;

/** Permission nodes supplement clan roles; granting a node never grants leadership. */
public final class ClanPermissionPolicy {
    private ClanPermissionPolicy() {}
    public static String command(String name) {
        String canonical = switch (name.toLowerCase(Locale.ROOT)) {
            case "aliance" -> "alliance";
            case "disban" -> "disband";
            default -> name.toLowerCase(Locale.ROOT);
        };
        return canonical.equals("chat") ? "nagaclan.chat" : "nagaclan.command." + canonical;
    }
    public static boolean allowed(String command, Predicate<String> permissions) { return permissions.test(command(command)); }
    public static String action(ClanMenuAction action) {
        return switch (action) {
            case OPEN_DASHBOARD, OPEN_MEMBERS, OPEN_MEMBER, OPEN_BANK, OPEN_ICONS,
                    SETTINGS, BANK, BACK, BANK_SETTINGS -> command("menu");
            case OPEN_HOMES, HOMES, HOME_TELEPORT -> command("home");
            case OPEN_TOP -> command("top");
            case OPEN_SETTINGS -> command("settings");
            case OPEN_ALLIANCES -> command("menu");
            case MEMBER_KICK -> command("kick");
            case SET_BUILDER -> command("setbuilder");
            case SET_ADMIN -> command("setadmin");
            case SET_LEADER -> command("setleader");
            case LEAVE -> command("leave");
            case DISBAND_CONFIRM -> command("disband");
            case BANK_DEPOSIT, DEPOSIT, DEPOSIT_AMOUNT -> "nagaclan.bank.deposit";
            case BANK_WITHDRAW, WITHDRAW, WITHDRAW_AMOUNT -> "nagaclan.bank.withdraw";
            case TOGGLE_MEMBER_DEPOSIT, TOGGLE_MEMBER_WITHDRAW -> "nagaclan.bank.settings";
            case EDIT_PREFIX, SELECT_ICON, CUSTOM_ICON, OPEN_ACCESS, CYCLE_ACCESS, TOGGLE_TOP_VISIBILITY, TOGGLE_FRIENDLY_FIRE -> "nagaclan.settings";
            case CREATE_HELP -> command("create");
            case ACCEPT_HELP -> command("accept");
            case DENY_HELP -> command("deny");
            default -> null; // Close/navigation and confirmation have checks at their destination/action.
        };
    }
}
