package cc.vanishclient.gui.icons;

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
        return "assets/vanishclient/textures/icons/" + resourceName + ".svg";
    }
}
