package com.antilagaddon.modules;

import com.antilagaddon.AntiLagAddon;
import com.antilagaddon.perf.AntiLagConfig;
import com.antilagaddon.perf.DiagnosticsTracker;
import com.antilagaddon.perf.PerformanceMonitor;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/**
 * Lightweight diagnostics module.
 *
 * <p>Measures FPS, frame time, client tick time, entity counts and observable packet rates, plus
 * chunk/block update workload and recent tick spikes. It is ambient and read-only: it never cancels
 * inbound packets, drops chunk/entity updates or blocks networking threads.</p>
 */
public class LagMonitor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> logSpikes = sgGeneral.add(new BoolSetting.Builder()
        .name("log-spikes")
        .description("Log a warning to chat when a client tick exceeds 100 ms.")
        .defaultValue(false)
        .build()
    );

    private boolean ownsSampling;
    private long lastSpikeLog;

    public LagMonitor() {
        super(AntiLagAddon.CATEGORY, "lag-monitor", "Lightweight FPS, frame-time, tick-time, entity-count and packet-rate diagnostics.");
    }

    @Override
    public void onActivate() {
        DiagnosticsTracker.INSTANCE.reset();
        DiagnosticsTracker.INSTANCE.subscribe();

        // The main module already samples performance when it is running.
        ownsSampling = !AntiLagConfig.masterEnabled;
        if (ownsSampling) PerformanceMonitor.INSTANCE.addConsumer();
    }

    @Override
    public void onDeactivate() {
        DiagnosticsTracker.INSTANCE.unsubscribe();
        if (ownsSampling) {
            PerformanceMonitor.INSTANCE.removeConsumer();
            ownsSampling = false;
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (ownsSampling) PerformanceMonitor.INSTANCE.sampleFrame();

        if (!logSpikes.get()) return;

        double tickMs = DiagnosticsTracker.INSTANCE.lastTickMs();
        long now = System.currentTimeMillis();
        // Throttle chat logging to at most once every 5 seconds.
        if (tickMs > 100 && now - lastSpikeLog > 5000) {
            lastSpikeLog = now;
            warning("Tick spike detected: %.0f ms (%.0f FPS, %d entities).",
                tickMs, PerformanceMonitor.INSTANCE.fps(), PerformanceMonitor.INSTANCE.entityCount());
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        DiagnosticsTracker.INSTANCE.reset();
        PerformanceMonitor.INSTANCE.reset();
    }

    @Override
    public String getInfoString() {
        if (!isActive()) return null;
        float fps = PerformanceMonitor.INSTANCE.fps();
        double tick = PerformanceMonitor.INSTANCE.tickTimeMs();
        return String.format("%.0f FPS %.1fms", fps, tick);
    }
}
