package cc.vanishclient.gui.icons;

import cc.vanishclient.VanishClient;
import net.minecraft.util.Identifier;

public enum IconKey {
    ANCHOR("anchor"),
    APPLE("apple"),
    AXE("axe"),
    FOLDER_OPEN("folder-open"),
    FOLDER_PLUS("folder-plus"),
    GAVEL("gavel"),
    ORBIT("orbit"),
    PICKAXE("pickaxe"),
    SPARKLE("sparkle"),
    SPORT_SHOE("sport-shoe"),
    STAR_PLUS("star-plus"),
    SWORD("sword"),
    SWORDS("swords"),
    USER("user");

    private final String resourceName;

    IconKey(String resourceName) {
        this.resourceName = resourceName;
    }

    public String resourcePath() {
        return "assets/" + VanishClient.MOD_ID + "/textures/icons/" + resourceName + ".png";
    }

    public Identifier textureIdentifier() {
        return Identifier.of(VanishClient.MOD_ID, "textures/icons/" + resourceName + ".png");
    }
}
