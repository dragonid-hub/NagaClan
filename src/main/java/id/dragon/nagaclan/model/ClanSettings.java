package id.dragon.nagaclan.model;

public final class ClanSettings {
    private final java.util.EnumMap<ClanAccess, ClanRole> access = new java.util.EnumMap<>(ClanAccess.class);
    public ClanRole access(ClanAccess action) {
        if (access.containsKey(action)) return access.get(action);
        if (action == ClanAccess.DEPOSIT) return memberDepositAllowed ? ClanRole.MEMBER : ClanRole.LEADER;
        if (action == ClanAccess.WITHDRAW) return memberWithdrawAllowed ? ClanRole.MEMBER : ClanRole.LEADER;
        return action.defaultRole();
    }
    public boolean allows(ClanRole role, ClanAccess action) { return role != null && role.ordinal() <= access(action).ordinal(); }
    public void setAccess(ClanAccess action, ClanRole role) { access.put(java.util.Objects.requireNonNull(action), java.util.Objects.requireNonNull(role)); }
    private boolean friendlyFire;
    private boolean memberDepositAllowed = true;
    private boolean memberWithdrawAllowed;
    private boolean visibleOnTop = true;
    private String prefix = "";
    private String icon = "AMETHYST_SHARD";
    public boolean friendlyFire() { return friendlyFire; }
    public void setFriendlyFire(boolean value) { friendlyFire = value; }
    public boolean memberDepositAllowed() { return access(ClanAccess.DEPOSIT) == ClanRole.MEMBER; }
    public void setMemberDepositAllowed(boolean value) { memberDepositAllowed = value; access.remove(ClanAccess.DEPOSIT); }
    public boolean memberWithdrawAllowed() { return access(ClanAccess.WITHDRAW) == ClanRole.MEMBER; }
    public void setMemberWithdrawAllowed(boolean value) { memberWithdrawAllowed = value; access.remove(ClanAccess.WITHDRAW); }
    public boolean visibleOnTop() { return visibleOnTop; }
    public void setVisibleOnTop(boolean value) { visibleOnTop = value; }
    public String prefix() { return prefix; }
    public void setPrefix(String value) { prefix = value == null ? "" : value; }
    public String icon() { return icon; }
    public void setIcon(String value) { icon = value == null || value.isBlank() ? "AMETHYST_SHARD" : value; }
}
