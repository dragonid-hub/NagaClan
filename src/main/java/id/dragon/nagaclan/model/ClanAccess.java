package id.dragon.nagaclan.model;

/** Maximum (lowest-ranking) role allowed; leadership always retains access. */
public enum ClanAccess {
    CHAT(ClanRole.MEMBER), HOME(ClanRole.MEMBER), SET_HOME(ClanRole.BUILDER), DELETE_HOME(ClanRole.BUILDER),
    INVITE(ClanRole.ADMIN), KICK(ClanRole.ADMIN), ALLIANCE(ClanRole.ADMIN), SET_BUILDER(ClanRole.ADMIN),
    SET_ADMIN(ClanRole.LEADER), DEPOSIT(ClanRole.MEMBER), WITHDRAW(ClanRole.LEADER),
    PVP(ClanRole.LEADER), TOP_VISIBILITY(ClanRole.LEADER), PREFIX(ClanRole.LEADER), ICON(ClanRole.LEADER);

    private final ClanRole defaultRole;
    ClanAccess(ClanRole defaultRole) { this.defaultRole = defaultRole; }
    public ClanRole defaultRole() { return defaultRole; }
    public static ClanAccess command(String command) {
        return switch (command.toLowerCase(java.util.Locale.ROOT)) {
            case "chat" -> CHAT; case "home" -> HOME; case "sethome" -> SET_HOME; case "delhome" -> DELETE_HOME;
            case "invite" -> INVITE; case "kick" -> KICK; case "alliance", "aliance" -> ALLIANCE;
            case "setbuilder" -> SET_BUILDER; case "setadmin" -> SET_ADMIN;
            default -> null;
        };
    }
}
