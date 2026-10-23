package id.dragon.nagaclan.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** Shared by input prompts and clan chat, before any server-thread delivery is scheduled. */
public final class ClanChatPrivacy {
    private ClanChatPrivacy() {}
    public static void suppressPublicDelivery(AsyncChatEvent event) {
        event.setCancelled(true);
        event.viewers().clear();
    }
    public static Component label() { return Component.text("[Teams Chat] ", NamedTextColor.AQUA); }
}
