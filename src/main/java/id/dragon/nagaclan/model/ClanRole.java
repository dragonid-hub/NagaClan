package id.dragon.nagaclan.model;

public enum ClanRole {
    LEADER, ADMIN, BUILDER, MEMBER;

    public boolean canManageHomes() { return this != MEMBER; }
    public boolean canManageMembers() { return this == LEADER || this == ADMIN; }
    public boolean canManageSettings() { return this == LEADER; }
}
