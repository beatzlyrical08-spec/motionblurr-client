package cc.vanishclient.module.modules.movement;

import cc.vanishclient.event.impl.player.TickEvent;
import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import meteordevelopment.orbit.EventHandler;

public final class Sprint extends Module {

    public Sprint() {
        super("Sprint", "Makes you automatically sprint", -1, Category.MOVEMENT);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;
        if (mc.options.getSprintToggled().getValue()) mc.options.getSprintToggled().setValue(false);

        mc.options.sprintKey.setPressed(true);
    }
}

