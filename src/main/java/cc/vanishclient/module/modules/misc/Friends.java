package cc.vanishclient.module.modules.misc;

import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import cc.vanishclient.gui.FriendsScreen;

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

