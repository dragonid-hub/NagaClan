package id.dragon.nagaclan.command;

import java.util.List;

public final class ClanCommandSuggestions {
    private ClanCommandSuggestions() {
    }

    public static List<String> secondArgument(String subcommand, List<String> homeNames, List<String> clanNames, List<String> onlinePlayerNames) {
        return switch (subcommand.toLowerCase()) {
            case "create", "sethome" -> List.of(id.dragon.nagaclan.text.Messages.get("usage.name", "<nama>"));
            case "chat" -> List.of(id.dragon.nagaclan.text.Messages.get("usage.message", "<pesan>"));
            case "home", "delhome" -> homeNames;
            case "invite", "kick", "setbuilder", "setadmin", "setleader" -> onlinePlayerNames;
            case "alliance", "aliance" -> clanNames;
            default -> List.of();
        };
    }
}
