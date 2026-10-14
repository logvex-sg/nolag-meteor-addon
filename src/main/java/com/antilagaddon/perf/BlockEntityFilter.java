package com.antilagaddon.perf;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/**
 * Subscribes to Meteor's {@link RenderBlockEntityEvent} and cancels rendering of far block entities.
 *
 * <p>The event supplies a {@code BlockEntityRenderState} rather than the block entity itself, but
 * that state carries both the block-entity type and the block position, which is all the distance
 * test needs. No mixin is required.</p>
 */
public final class BlockEntityFilter {
    public static final BlockEntityFilter INSTANCE = new BlockEntityFilter();

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private int consumers;

    private BlockEntityFilter() {}

    public synchronized void addConsumer() {
        if (consumers++ == 0) MeteorClient.EVENT_BUS.subscribe(this);
    }

    public synchronized void removeConsumer() {
        if (consumers > 0 && --consumers == 0) MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    @EventHandler
    private void onRenderBlockEntity(RenderBlockEntityEvent event) {
        var state = event.blockEntityState;
        if (state == null) return;
        if (!AntiLagConfig.masterEnabled) return;
        if (!AntiLagConfig.get(AntiLagConfig.blockEntitiesEnabled)) return;

        BlockKind kind = BlockKind.of(state.type);
        if (kind == null || !kind.enabled()) return;

        double distance = kind.distance();
        if (distance <= 0) return;

        BlockPos pos = state.pos;
        var cam = mc.gameRenderer.getCamera().getCameraPos();
        double dx = pos.getX() + 0.5 - cam.x;
        double dy = pos.getY() + 0.5 - cam.y;
        double dz = pos.getZ() + 0.5 - cam.z;
        double limit = distance * AdaptiveController.INSTANCE.distanceFactor();

        if ((dx * dx + dy * dy + dz * dz) > (limit * limit)) event.cancel();
    }

    /** @return true when the given block entity would be suppressed (used by diagnostics). */
    public static boolean wouldSuppress(BlockEntity blockEntity) {
        return Suppression.shouldSuppressBlockEntity(blockEntity);
    }
}
