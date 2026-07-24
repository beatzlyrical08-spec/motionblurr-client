package cc.motionblurr.utils.render.nanovg;

public class NanoVGContext {
    private static boolean initialized = false;

    public static void init() {
        initialized = true;
    }

    public static void reinit() {
        cleanup();
        init();
    }

    public static boolean isValid() {
        return initialized;
    }

    public static void assertValid() {
        if (!isValid()) {
            throw new IllegalStateException("Invalid NanoVG context");
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void cleanup() {
        initialized = false;
    }
}


