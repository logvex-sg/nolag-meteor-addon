package com.antilagaddon.perf;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

/**
 * Budget enforcement and bounded scanning shared by the entity, particle and block-entity paths.
 *
 * <p>The entity budget keeps the nearest N suppressible entities visible and records the ids of the
 * rest. The set is rebuilt by a bounded, throttled scan instead of being counted during rendering,
 * so the decision is deterministic and independent of how many frames are drawn per tick. The
 * particle budget is a per-tick counter, since each particle is spawned and counted exactly once.</p>
 */
public final class LimiterManager {
    public static final LimiterManager INSTANCE = new LimiterManager();

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int MAX_CANDIDATES = 4096;

    private final IntOpenHashSet hiddenIds = new IntOpenHashSet();

    private long[] candidateBuffer = new long[256];
    private int candidateCount;

    private int particleCountThisTick;

    private LimiterManager() {}

    public void reset() {
        hiddenIds.clear();
        candidateCount = 0;
        particleCountThisTick = 0;
    }

    /** Called at the start of each client tick. */
    public void beginTick() {
        particleCountThisTick = 0;
    }

    /**
     * Rebuilds the set of render-budget-hidden entities. Bounded and safe on the client thread.
     *
     * @param enabled whether any module is currently applying the budget
     */
    public void rebuildEntityBudget(boolean enabled) {
        hiddenIds.clear();

        if (!enabled) return;

        int budget = AntiLagConfig.getInt(AntiLagConfig.entityBudget);
        if (budget <= 0) return;
        if (mc.world == null || mc.gameRenderer == null) return;

        budget = (int) Math.max(1, budget * AdaptiveController.INSTANCE.budgetFactor());

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

        candidateCount = 0;
        for (Entity entity : mc.world.getEntities()) {
            if (!Suppression.isSuppressibleCategory(entity)) continue;
            if (candidateCount >= MAX_CANDIDATES) break;

            if (candidateCount >= candidateBuffer.length) {
                candidateBuffer = Arrays.copyOf(candidateBuffer, candidateBuffer.length * 2);
            }

            double dx = entity.getX() - cam.x;
            double dy = entity.getY() - cam.y;
            double dz = entity.getZ() - cam.z;
            float distSq = (float) (dx * dx + dy * dy + dz * dz);

            // Pack a sortable key: distance bits in the high 32 bits, entity id in the low 32.
            long distBits = Float.floatToIntBits(distSq) & 0xFFFFFFFFL;
            candidateBuffer[candidateCount++] = (distBits << 32) | (entity.getId() & 0xFFFFFFFFL);
        }

        if (candidateCount <= budget) return;

        Arrays.sort(candidateBuffer, 0, candidateCount);

        for (int i = budget; i < candidateCount; i++) {
            hiddenIds.add((int) (candidateBuffer[i] & 0xFFFFFFFFL));
        }
    }

    /** @return true when this entity id is hidden because the render budget is exhausted. */
    public boolean isHiddenByBudget(int entityId) {
        return !hiddenIds.isEmpty() && hiddenIds.contains(entityId);
    }

    /** Counts a particle spawned this tick and reports whether the per-tick budget is exceeded. */
    public boolean countParticle() {
        int budget = AntiLagConfig.getInt(AntiLagConfig.particleBudget);
        if (budget <= 0) return false;

        particleCountThisTick++;
        int limit = (int) Math.max(1, budget * AdaptiveController.INSTANCE.budgetFactor());
        return particleCountThisTick > limit;
    }

    public int particleCountThisTick() { return particleCountThisTick; }

    /** @return the number of block entities currently hidden, for diagnostics. */
    public int hiddenEntityCount() { return hiddenIds.size(); }

    /** Static convenience used to decide whether the block-entity budget path is active. */
    public static boolean blockBudgetActive() {
        return AntiLagConfig.get(AntiLagConfig.blockEntitiesEnabled);
    }
}
