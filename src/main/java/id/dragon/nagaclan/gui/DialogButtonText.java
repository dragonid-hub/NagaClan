package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.text.SmallCaps;

/** Short symbols are legible in vanilla's text-only dialog buttons. */
final class DialogButtonText {
    private DialogButtonText() { }

    static String label(ClanMenuAction action, String text) {
        boolean dynamic = action == ClanMenuAction.OPEN_MEMBER || action == ClanMenuAction.HOME_TELEPORT
                || action == ClanMenuAction.SELECT_ICON;
        String label = dynamic ? text : SmallCaps.format(text);
        String symbol = switch (action) {
            case OPEN_MEMBERS, OPEN_MEMBER -> "♟";
            case OPEN_HOMES, HOME_TELEPORT -> "⌂";
            case OPEN_BANK, BANK_DEPOSIT, BANK_WITHDRAW -> "◆";
            case OPEN_ALLIANCES -> "⚑";
            case OPEN_SETTINGS, OPEN_ACCESS, CYCLE_ACCESS -> "⚙";
            case OPEN_TOP -> "✦";
            case OPEN_ICONS, CUSTOM_ICON, SELECT_ICON -> "◈";
            case EDIT_PREFIX, CREATE_HELP -> "✎";
            case PREVIOUS_PAGE -> "◀";
            case NEXT_PAGE -> "▶";
            case OPEN_DASHBOARD -> "⌂";
            case LEAVE, DISBAND_CONFIRM, MEMBER_KICK -> "✕";
            case CONFIRM_ACTION, ACCEPT_HELP -> "✔";
            case DENY_HELP -> "✕";
            default -> "";
        };
        return symbol.isEmpty() ? label : symbol + " " + label;
    }
}
