package cc.motionblurr.module.modules.misc;

import cc.motionblurr.module.Category;
import cc.motionblurr.module.Module;
import cc.motionblurr.gui.FriendsScreen;

public class Friends extends Module {

    public Friends() {
        super("Friends", "Manage your friends list", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            mc.setScreen(new FriendsScreen());
        }
        setEnabled(false);
    }
}

