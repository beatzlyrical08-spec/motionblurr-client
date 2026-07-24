package cc.motionblurr.gui.modern;

import java.util.HashMap;
import java.util.Map;

public final class AnimationManager {
    private final Map<String, Float> values = new HashMap<>();
    private float openProgress = 0f;
    private boolean closing;

    public void update(float delta, boolean closing) {
        this.closing = closing;
        if (closing) {
            openProgress = Math.max(0f, openProgress - delta * 5.2f);
        } else {
            openProgress = Math.min(1f, openProgress + delta * 5.2f);
        }
    }

    public float getOpenProgress() {
        return openProgress;
    }

    public float getScaledOpenProgress() {
        return easeOutCubic(openProgress);
    }

    public float getValue(String key, float target) {
        return values.compute(key, (ignored, current) -> current == null ? target : current + (target - current) * 0.2f);
    }

    public void setValue(String key, float value) {
        values.put(key, value);
    }

    public void reset(String key) {
        values.remove(key);
    }

    public boolean isClosing() {
        return closing;
    }

    public static float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3f);
    }
}
