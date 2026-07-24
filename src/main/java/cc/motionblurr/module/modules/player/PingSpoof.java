package cc.motionblurr.module.modules.player;


import cc.motionblurr.event.impl.network.PacketEvent;
import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import cc.motionblurr.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class PingSpoof extends Module {
    private final NumberSetting msDelay = new NumberSetting("Ms", 1, 500, 60, 1);

    public PingSpoof() {
        super("Ping Spoof", "Increases your ping", -1, Category.PLAYER);
        this.addSetting(msDelay);
    }

    @EventHandler
    private void onEventPacket(PacketEvent event) {
        if (event.getPacket() instanceof KeepAliveS2CPacket packet) {
            if (isNull()) return;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(msDelay.getValueInt());
                    Objects.requireNonNull(mc.getNetworkHandler()).getConnection().send(new KeepAliveC2SPacket(packet.getId()));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            future.join();
        }
    }
}

