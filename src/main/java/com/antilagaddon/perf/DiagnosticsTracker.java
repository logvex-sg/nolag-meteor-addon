package com.antilagaddon.perf;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

/**
 * Safe, read-only diagnostics.
 *
 * <p>Counts observable update workload (chunk data and block updates), tracks client tick spikes
 * and keeps a bounded record of the most recent spikes. It never cancels or reorders packets and
 * never touches the networking threads.</p>
 */
public final class DiagnosticsTracker {
    public static final DiagnosticsTracker INSTANCE = new DiagnosticsTracker();

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final int SPARK_RING = 8;

    private volatile int chunksReceived;
    private volatile int blockUpdates;
    private volatile int lastChunkCount;
    private volatile int lastBlockCount;

    private volatile double lastTickMs;
    private volatile double spikePeak;
    private volatile long spikeTick = Long.MIN_VALUE;

    private final double[] sparkRing = new double[SPARK_RING];
    private int sparkIndex;
    private int sparkCount;

    private DiagnosticsTracker() {}

    public void reset() {
        chunksReceived = 0;
        blockUpdates = 0;
        lastChunkCount = 0;
        lastBlockCount = 0;
        lastTickMs = 0;
        spikePeak = 0;
        spikeTick = Long.MIN_VALUE;
        sparkIndex = 0;
        sparkCount = 0;
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        chunksReceived++;
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        blockUpdates++;
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        double tickMs = PerformanceMonitor.INSTANCE.tickTimeMs();
        lastTickMs = tickMs;

        long now = System.currentTimeMillis();
        // A spike is a tick whose duration is more than 50 ms (below 20 TPS).
        if (tickMs > 50) {
            spikePeak = Math.max(spikePeak, tickMs);
            spikeTick = now;

            sparkRing[sparkIndex] = tickMs;
            sparkIndex = (sparkIndex + 1) % SPARK_RING;
            if (sparkCount < SPARK_RING) sparkCount++;
        }

        if (mc.world == null) {
            lastChunkCount = 0;
            lastBlockCount = 0;
            return;
        }

        lastChunkCount = chunksReceived;
        lastBlockCount = blockUpdates;
    }

    public double lastTickMs() { return lastTickMs; }

    public double spikePeak() { return spikePeak; }

    public boolean recentSpike(long withinMillis) {
        return spikeTick != Long.MIN_VALUE && (System.currentTimeMillis() - spikeTick) <= withinMillis;
    }

    public int sparkCount() { return sparkCount; }

    public double sparkAt(int i) { return sparkRing[i % SPARK_RING]; }

    public int chunksReceived() { return lastChunkCount; }

    public int blockUpdates() { return lastBlockCount; }

    public void noteSpike(double ms) {
        spikePeak = ms;
        spikeTick = System.currentTimeMillis();
    }

    /** Registers the diagnostics event handlers on Meteor's event bus. */
    public void subscribe() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void unsubscribe() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }
}
