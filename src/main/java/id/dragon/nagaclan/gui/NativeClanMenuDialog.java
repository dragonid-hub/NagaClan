package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.text.ClanColors;
import id.dragon.nagaclan.text.Messages;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/** Native 1.21.6+ view of the same holder-bound actions used by the GUI. */
final class NativeClanMenuDialog {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private NativeClanMenuDialog() { }

    static void show(Player player, Clan clan, Inventory inventory, Consumer<MenuAction> click) {
        ClanMenuHolder holder = (ClanMenuHolder) inventory.getHolder();
        Component title = Component.text("NagaClan", TextColor.color(0x6D5DFB))
                .append(Component.text("  •  " + Messages.get("menu." + pageKey(holder.page())), TextColor.color(0x4CC9F0)));
        var role = clan == null ? null : clan.roleOf(player.getUniqueId());
        String subtitle = clan == null ? Messages.get("dialog.no-clan")
                : clan.name() + (role == null ? "" : "  •  " + Messages.get("role." + role.name()));
        ItemStack head = playerHead(clan == null ? player.getUniqueId() : clan.leaderId(), clan == null ? player.getName() : clan.name());
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.item(head)
                .description(DialogBody.plainMessage(Component.text(subtitle, TextColor.color(0xA9B8FF))))
                .showTooltip(true).width(48).height(48).build());
        body.add(DialogBody.plainMessage(Component.text(Messages.get("dialog.slogan"), TextColor.color(0x8B7BFF))));
        List<ActionButton> buttons = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            MenuAction action = holder.actionAt(slot).orElse(null);
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            if (action != null && action.type() != ClanMenuAction.NONE && action.type() != ClanMenuAction.CLOSE)
                buttons.add(button(player, item, action, click));
            else if (action == null || action.type() == ClanMenuAction.NONE) {
                Component summary = summary(item, holder.page());
                if (summary != null) body.add(DialogBody.plainMessage(summary));
            }
        }
        ActionButton close = ActionButton.builder(Component.text(Messages.get("dialog.close"), TextColor.color(0xA9B8FF))).build();
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(body)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .canCloseWithEscape(true).build())
                .type(buttons.isEmpty() ? DialogType.notice(close) : DialogType.multiAction(buttons, close, 2))));
    }

    private static ActionButton button(Player player, ItemStack item, MenuAction action, Consumer<MenuAction> click) {
        ItemMeta meta = item.getItemMeta();
        String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();
        List<String> lore = meta != null && meta.hasLore() ? meta.getLore() : List.of();
        if (lore != null && !lore.isEmpty() && (action.type() == ClanMenuAction.OPEN_MEMBER
                || action.type() == ClanMenuAction.HOME_TELEPORT
                || action.type() == ClanMenuAction.BANK_DEPOSIT || action.type() == ClanMenuAction.BANK_WITHDRAW)) {
            name += " &8• &f" + lore.getFirst();
        }
        Component tooltip = Component.empty();
        if (lore != null) for (String line : lore) {
            if (!tooltip.equals(Component.empty())) tooltip = tooltip.append(Component.newline());
            tooltip = tooltip.append(colored(line));
        }
        var options = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofSeconds(60)).build();
        var dialogAction = DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player actor && actor.getUniqueId().equals(player.getUniqueId())) click.accept(action);
        }, options);
        ActionButton.Builder builder = ActionButton.builder(colored(name)).action(dialogAction);
        if (!tooltip.equals(Component.empty())) builder.tooltip(tooltip);
        return builder.build();
    }

    private static ItemStack playerHead(UUID owner, String name) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        meta.displayName(Component.text(name, TextColor.color(0x4CC9F0)));
        item.setItemMeta(meta);
        return item;
    }

    private static Component summary(ItemStack item, ClanMenuPage page) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        String name = meta.getDisplayName();
        if (name.isBlank()) return null;
        List<String> lore = meta.hasLore() ? meta.getLore() : List.of();
        Component line = colored("&8• &7" + name);
        if (lore != null && !lore.isEmpty()) {
            int count = page == ClanMenuPage.MAIN || page == ClanMenuPage.BANK ? lore.size() : Math.min(1, lore.size());
            for (int i = 0; i < count; i++) line = line.append(Component.newline()).append(colored("  &7" + lore.get(i)));
        }
        return line;
    }

    private static Component colored(String value) { return LEGACY.deserialize(ClanColors.colorize(value)); }

    private static String pageKey(ClanMenuPage page) {
        return switch (page) {
            case MAIN -> "main";
            case MEMBERS -> "members";
            case HOMES -> "homes";
            case BANK, DEPOSIT, WITHDRAW, BANK_SETTINGS -> "bank";
            case ALLIANCES -> "alliances";
            case SETTINGS -> "settings";
            case ACCESS -> "access";
            case ICONS -> "icons";
            case TOP -> "top";
        };
    }
}
