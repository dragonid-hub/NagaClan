package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.model.ClanRole;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Pure role hierarchy for member-management menu actions. */
public final class RoleActionPolicy {
    private RoleActionPolicy() {
    }
    public static Set<ClanMenuAction> actionsFor(id.dragon.nagaclan.model.Clan clan, java.util.UUID actorId, java.util.UUID targetId) {
        ClanRole actor = clan.roleOf(actorId), target = clan.roleOf(targetId);
        if (actor == null || target == null || actorId.equals(targetId) || actor.ordinal() >= target.ordinal()) return Set.of();
        EnumSet<ClanMenuAction> result = EnumSet.noneOf(ClanMenuAction.class);
        if (clan.settings().allows(actor, id.dragon.nagaclan.model.ClanAccess.KICK)) result.add(ClanMenuAction.MEMBER_KICK);
        if (actor.ordinal() < ClanRole.BUILDER.ordinal() && clan.settings().allows(actor, id.dragon.nagaclan.model.ClanAccess.SET_BUILDER)) result.add(ClanMenuAction.SET_BUILDER);
        if (actor == ClanRole.LEADER) { result.add(ClanMenuAction.SET_ADMIN); result.add(ClanMenuAction.SET_LEADER); }
        return Set.copyOf(result);
    }

    public static Set<ClanMenuAction> actionsFor(ClanRole actor, ClanRole target, boolean self) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(target, "target");
        if (self || (actor != ClanRole.LEADER && actor != ClanRole.ADMIN) || actor.ordinal() >= target.ordinal()) {
            return Set.of();
        }

        EnumSet<ClanMenuAction> actions = EnumSet.of(ClanMenuAction.MEMBER_KICK, ClanMenuAction.SET_BUILDER);
        if (actor == ClanRole.LEADER) {
            actions.add(ClanMenuAction.SET_ADMIN);
            actions.add(ClanMenuAction.SET_LEADER);
        }
        return Set.copyOf(actions);
    }
}
