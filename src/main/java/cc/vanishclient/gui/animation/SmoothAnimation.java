package cc.vanishclient.gui.animation;

public final class SmoothAnimation {
    private SmoothAnimation() {
    }

    public static float approach(float current, float target, float speed, float deltaSeconds) {
        float delta = clamp(deltaSeconds, 0.0F, 0.05F);
        float factor = 1.0F - (float) Math.exp(-Math.max(0.0F, speed) * delta);
        float next = current + (target - current) * factor;
        return Math.abs(next - target) < 0.003F ? target : next;
    }

    public static float smoothstep(float value) {
        float t = clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
