package com.antilagaddon.perf;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight, allocation-free performance sampling for the AntiLag addon.
 *
 * <p>The monitor observes only cheap client-side signals: rendered FPS, Meteor's frame time, client
 * tick duration, entity counts and inbound/outbound packet rates. It never touches or delays the
 * networking pipeline: packet counts are bumped with atomics on the netty thread and read on the
 * client thread once per tick.</p>
 */
public final class PerformanceMonitor {
    public static final PerformanceMonitor INSTANCE = new PerformanceMonitor();

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /** Bounded history for the diagnostics HUD. */
    private static final int HISTORY_SIZE = 120;

    private final float[] fpsHistory = new float[HISTORY_SIZE];
    private int historyIndex;
    private int historyCount;

    private final AtomicInteger packetsIn = new AtomicInteger();
    private final AtomicInteger packetsOut = new AtomicInteger();

    private volatile int packetRateIn;
    private volatile int packetRateOut;
    private volatile float fps;
    private volatile double frameTimeMs;
    private volatile double tickTimeMs;

    private long tickStartNanos;
    private int tickCounter;
    private int ticksSinceLevelChange;
    private int cumulativeTicks;

    private int entityCount;
    private int loadDistanceChunks;

    private volatile boolean streaming = true;

    private int consumers;

    private PerformanceMonitor() {}

    /** Reference-counted subscription so several modules can share one event registration. */
    public synchronized void addConsumer() {
        if (consumers++ == 0) MeteorClient.EVENT_BUS.subscribe(this);
    }

    public synchronized void removeConsumer() {
        if (consumers > 0 && --consumers == 0) MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    // ---- lifecycle -------------------------------------------------------

    public void reset() {
        historyIndex = 0;
        historyCount = 0;
        packetsIn.set(0);
        packetsOut.set(0);
        packetRateIn = 0;
        packetRateOut = 0;
        ticksSinceLevelChange = 0;
        cumulativeTicks = 0;
        entityCount = 0;
        tickCounter = 0;
    }

    // ---- event hooks -----------------------------------------------------

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        packetsIn.lazySet(packetsIn.get() + 1);
    }

    @EventHandler
    private void onPacketSent(PacketEvent.Sent event) {
        packetsOut.lazySet(packetsOut.get() + 1);
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        tickStartNanos = System.nanoTime();
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        long now = System.nanoTime();
        double measured = (now - tickStartNanos) / 1_000_000.0;
        // Exponential moving average smooths the tick duration spikes.
        tickTimeMs = tickTimeMs == 0 ? measured : tickTimeMs * 0.7 + measured * 0.3;

        tickCounter++;
        cumulativeTicks++;

        if (tickCounter >= 20) {
            tickCounter = 0;
            packetRateIn = packetsIn.getAndSet(0);
            packetRateOut = packetsOut.getAndSet(0);
            sampleCounts();
        }

        if (mc.isInSingleplayer()) streaming = false;
        else streaming = true;
    }

    /** Samples FPS (cheap) once per client tick. */
    public void sampleFrame() {
        if (mc == null) return;

        float currentFps = mc.getCurrentFps();
        fps = currentFps;
        // Meteor maintains a smoothed frame time each frame; prefer it over a value derived from FPS.
        double meteorFrameMs = Utils.frameTime * 1000.0;
        frameTimeMs = meteorFrameMs > 0 ? meteorFrameMs : (currentFps <= 0 ? 0 : 1000.0 / currentFps);

        fpsHistory[historyIndex] = currentFps;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;
        if (historyCount < HISTORY_SIZE) historyCount++;
    }

    private void sampleCounts() {
        if (mc.world == null) {
            entityCount = 0;
            return;
        }

        entityCount = mc.world.getRegularEntityCount();
        loadDistanceChunks = mc.options.getClampedViewDistance();
    }

    // ---- adaptive level bookkeeping -------------------------------------

    public int ticksSinceLevelChange() { return ticksSinceLevelChange; }
    public int cumulativeTicks() { return cumulativeTicks; }
    public void markLevelChanged() { ticksSinceLevelChange = 0; }

    // ---- getters ---------------------------------------------------------

    public float fps() { return fps; }
    public double frameTimeMs() { return frameTimeMs; }
    public double tickTimeMs() { return tickTimeMs; }
    public int entityCount() { return entityCount; }
    public int packetRateIn() { return packetRateIn; }
    public int packetRateOut() { return packetRateOut; }
    public int loadDistanceChunks() { return loadDistanceChunks; }
    public boolean isStreaming() { return streaming; }

    /** @return average FPS over the recorded history, or 0 when no samples exist. */
    public float averageFps() {
        if (historyCount == 0) return 0;
        float sum = 0;
        for (int i = 0; i < historyCount; i++) sum += fpsHistory[i];
        return sum / historyCount;
    }

    public boolean historyReady() { return historyCount >= 20; }

    /** @return the most recent frame time, or -1 if there is no sample yet. */
    public double latestFrameTime(double fallback) {
        return frameTimeMs == 0 ? fallback : frameTimeMs;
    }
}
