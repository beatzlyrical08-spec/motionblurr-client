package cc.vanishclient.module.modules.player;

import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import cc.vanishclient.module.setting.NumberSetting;

public class FastMine extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", 1, 10, 5, 0.5);

    public FastMine() {
        super("Fast Mine", "Mine blocks faster", -1, Category.PLAYER);
        addSettings(speed);
    }

    public float getSpeed() {
        return speed.getValueFloat();
    }
}

