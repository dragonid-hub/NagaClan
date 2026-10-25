package id.dragon.nagaclan.gui;

import id.dragon.nagaclan.text.Messages;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import java.time.Duration;
import id.dragon.nagaclan.text.SmallCaps;
import java.util.function.Consumer;

/** Loaded only after checking Paper API availability and the actual client protocol. */
final class NativeInputDialog {
    private NativeInputDialog() {}
    static void show(Player player, String title, String initial, Consumer<String> submit) {
        var options = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofSeconds(60)).build();
        var done = DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player actor && actor.getUniqueId().equals(player.getUniqueId())) submit.accept(view.getText("value"));
        }, options);
        var cancel = DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player actor && actor.getUniqueId().equals(player.getUniqueId())) submit.accept(null);
        }, options);
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title, TextColor.color(0x4CC9F0)))
                        .inputs(java.util.List.of(DialogInput.text("value", Component.text(title)).initial(initial).maxLength(256).build()))
                        .canCloseWithEscape(true).build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text(SmallCaps.format(Messages.get("input.done")), TextColor.color(0x4CC9F0))).action(done).build(),
                        ActionButton.builder(Component.text(SmallCaps.format(Messages.get("input.cancel")), TextColor.color(0xFF5C8A))).action(cancel).build()))));
    }
}
