package cc.motionblurr.module.modules.client;

import cc.motionblurr.event.impl.network.PacketEvent;
import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import cc.motionblurr.utils.mc.ChatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;

public class Debugger extends Module {
    public Debugger() {
        super("Debugger", "Debugs inv packets (dev purposes)", -1, Category.CLIENT);
    }

    @EventHandler
    public void onPacketSend(PacketEvent e) {
        if (isNull()) return;
        if (e.getPacket() == null) return;
        if (!(e.getPacket() instanceof ClickSlotC2SPacket packet))
            return;

        ChatUtil.addChatMessage("""
                ClickSlotPacket
                  syncId: %s
                  revision: %s
                  slot: %s
                  button: %s
                  actionType: %s
                  modifiedItems: %s
                  stack: %s
                """.formatted(
                packet.getSyncId(),
                packet.getRevision(),
                packet.getSlot(),
                packet.getButton(),
                packet.getActionType(),
                packet.getModifiedStacks(),
                packet.getStack()
        ));
    }
}

