package cc.vanishclient.module.modules.client;

import cc.vanishclient.module.Category;
import cc.vanishclient.module.Module;
import cc.vanishclient.module.setting.BooleanSetting;

public final class Client extends Module {

    public static final BooleanSetting title = new BooleanSetting("Title", true);
    public static final BooleanSetting showCommandSuggestions = new BooleanSetting("Show command suggestions", true);

    public Client() {
        super("Client", "Settings for the client", -1, Category.CLIENT);

        this.addSettings(
                title,
                showCommandSuggestions
        );
    }

    public boolean getTitle() {
        return title.getValue();
    }

    public boolean showCommandSuggestions() {
        return showCommandSuggestions.getValue();
    }
}

