package com.antilagaddon.perf;

import net.minecraft.client.MinecraftClient;

/**
 * Drives the once-per-tick update path shared by the AntiLag modules.
 *
 * <p>Work is intentionally cheap: sampling, adaptive evaluation and — at most every
 * {@code update-scan-interval} ticks — a single bounded entity scan for the render budget. No
 * full-world scan happens per frame.</p>
 */
public final class SuppressTickController {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private int tickCounter;

    /** Resets per-tick counters. Called at the start of each client tick. */
    public void beginTick() {
        LimiterManager.INSTANCE.beginTick();
    }

    /** Samples performance and, at most every scan-interval ticks, rebuilds the entity budget. */
    public void tick() {
        PerformanceMonitor.INSTANCE.sampleFrame();
        AdaptiveController.INSTANCE.tick();

        int interval = Math.max(1, AntiLagConfig.getInt(AntiLagConfig.updateScanInterval));
        tickCounter++;
        if (tickCounter >= interval) {
            tickCounter = 0;
            boolean budgetActive = AntiLagConfig.masterEnabled
                && AntiLagConfig.get(AntiLagConfig.entitiesEnabled)
                && AntiLagConfig.getInt(AntiLagConfig.entityBudget) > 0;
            LimiterManager.INSTANCE.rebuildEntityBudget(budgetActive);
        }
    }

    public void reset() {
        tickCounter = 0;
        LimiterManager.INSTANCE.reset();
        PerformanceMonitor.INSTANCE.reset();
        AdaptiveController.INSTANCE.reset();
    }

    public boolean inWorld() {
        return mc.world != null && mc.player != null;
    }
}
