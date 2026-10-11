package com.antilagaddon.perf;

/**
 * Drives the adaptive performance level from the signals collected by {@link PerformanceMonitor}.
 *
 * <p>Thresholds are applied with hysteresis: moving to a noisier level requires the degraded
 * condition to persist for {@code upgradeCooldown} ticks, and easing back requires a longer
 * {@code downgradeCooldown}. This prevents rapid level flapping around a threshold.</p>
 *
 * <p>The controller is purely advisory; callers read {@link #severity()}, {@link #budgetFactor()}
 * and {@link #distanceFactor()} and decide how (and whether) to apply them.</p>
 */
public final class AdaptiveController {
    public static final AdaptiveController INSTANCE = new AdaptiveController();

    private static final double[] BUDGET_FACTORS = {1.0, 0.70, 0.45, 0.25};
    private static final double[] DISTANCE_FACTORS = {1.0, 0.90, 0.75, 0.55};

    private volatile Level level = Level.Normal;
    private Level pending = Level.Normal;
    private int pendingTicks;

    private AdaptiveController() {}

    /** Called once per client tick from the AntiLag module. */
    public void tick() {
        if (!AntiLagConfig.get(AntiLagConfig.adaptiveEnabled)) {
            setLevel(Level.Normal);
            pending = Level.Normal;
            pendingTicks = 0;
            return;
        }

        PerformanceMonitor monitor = PerformanceMonitor.INSTANCE;
        if (!monitor.historyReady()) return;

        float fps = monitor.averageFps();
        double emergency = AntiLagConfig.getDouble(AntiLagConfig.emergencyFps);
        double performance = AntiLagConfig.getDouble(AntiLagConfig.performanceFps);
        double balanced = AntiLagConfig.getDouble(AntiLagConfig.balancedFps);
        double normal = AntiLagConfig.getDouble(AntiLagConfig.normalFps);

        Level desired;
        if (fps < emergency) desired = Level.Emergency;
        else if (fps < performance) desired = Level.Performance;
        else if (fps < balanced) desired = Level.Balanced;
        else if (fps >= normal) desired = Level.Normal;
        else desired = level; // inside the hysteresis band, keep the current level

        if (desired == level) {
            pending = level;
            pendingTicks = 0;
            return;
        }

        boolean escalating = desired.severity() > level.severity();
        int cooldown = escalating
            ? AntiLagConfig.getInt(AntiLagConfig.upgradeCooldown)
            : AntiLagConfig.getInt(AntiLagConfig.downgradeCooldown);

        if (desired != pending) {
            pending = desired;
            pendingTicks = 0;
        }

        pendingTicks++;
        if (pendingTicks >= cooldown) {
            setLevel(pending);
            pending = level;
            pendingTicks = 0;
        }
    }

    private void setLevel(Level newLevel) {
        if (newLevel == level) return;
        level = newLevel;
        PerformanceMonitor.INSTANCE.markLevelChanged();
    }

    /** @return true when the adaptive controller is driving the level. */
    public boolean active() {
        return AntiLagConfig.get(AntiLagConfig.adaptiveEnabled);
    }

    public Level level() { return level; }

    public int severity() { return active() ? level.severity() : 0; }

    public double budgetFactor() { return BUDGET_FACTORS[severity()]; }

    public double distanceFactor() { return DISTANCE_FACTORS[severity()]; }

    public void reset() {
        level = Level.Normal;
        pending = Level.Normal;
        pendingTicks = 0;
    }
}
