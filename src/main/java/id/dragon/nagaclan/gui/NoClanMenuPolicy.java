package id.dragon.nagaclan.gui;

import java.util.Set;

public final class NoClanMenuPolicy {
    private NoClanMenuPolicy() {}
    public static Set<ClanMenuAction> actions() {
        return Set.of(ClanMenuAction.CREATE_HELP, ClanMenuAction.ACCEPT_HELP, ClanMenuAction.DENY_HELP);
    }
}
