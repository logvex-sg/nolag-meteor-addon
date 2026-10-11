package com.antilagaddon.hud;

import com.antilagaddon.AntiLagAddon;
import com.antilagaddon.perf.DiagnosticsTracker;
import com.antilagaddon.perf.PerformanceMonitor;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Diagnostics HUD element.
 *
 * <p>Shows FPS, frame time, client tick time, entity count and inbound packet rate. It samples the
 * cheap FPS counters on render so it stays live even when no module is active.</p>
 */
public class LagMonitorHud extends HudElement {
    public static final HudElementInfo<LagMonitorHud> INFO = new HudElementInfo<>(
        AntiLagAddon.HUD_GROUP,
        "lag-monitor",
        "Displays FPS, frame time, tick time, entity count and packet rate.",
        LagMonitorHud::new
    );

    private static final Color BACKGROUND = new Color(0, 0, 0, 120);
    private static final Color TEXT = new Color(230, 245, 240);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showFrameTime = sgGeneral.add(new BoolSetting.Builder()
        .name("frame-time")
        .description("Show the current frame time in milliseconds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showTickTime = sgGeneral.add(new BoolSetting.Builder()
        .name("tick-time")
        .description("Show the client tick time in milliseconds.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("entities")
        .description("Show the current entity count.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("packets")
        .description("Show the inbound packet rate per second.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Render a shadow behind the text.")
        .defaultValue(true)
        .build()
    );

    private final StringBuilder builder = new StringBuilder();

    public LagMonitorHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        PerformanceMonitor monitor = PerformanceMonitor.INSTANCE;
        monitor.sampleFrame();

        double width = 0;
        double lineHeight = renderer.textHeight(shadow.get(), 1) + 2;
        double height = 0;

        width = Math.max(width, renderer.textWidth(line("FPS: %.0f", monitor.fps()), shadow.get(), 1));
        height += lineHeight;

        if (showFrameTime.get()) {
            width = Math.max(width, renderer.textWidth(line("Frame: %.1f ms", monitor.frameTimeMs()), shadow.get(), 1));
            height += lineHeight;
        }

        if (showTickTime.get()) {
            width = Math.max(width, renderer.textWidth(line("Tick: %.1f ms", monitor.tickTimeMs()), shadow.get(), 1));
            height += lineHeight;
        }

        if (showEntities.get()) {
            width = Math.max(width, renderer.textWidth(line("Entities: %d", monitor.entityCount()), shadow.get(), 1));
            height += lineHeight;
        }

        if (showPackets.get()) {
            width = Math.max(width, renderer.textWidth(line("In: %d/s", monitor.packetRateIn()), shadow.get(), 1));
            height += lineHeight;
        }

        double pad = 3;
        setSize(width + pad * 2, height + pad * 2);
        renderer.quad(x, y, getWidth(), getHeight(), BACKGROUND);

        double ty = y + pad;
        renderer.text(line("FPS: %.0f", monitor.fps()), x + pad, ty, TEXT, shadow.get(), 1);
        ty += lineHeight;

        if (showFrameTime.get()) {
            renderer.text(line("Frame: %.1f ms", monitor.frameTimeMs()), x + pad, ty, TEXT, shadow.get(), 1);
            ty += lineHeight;
        }
        if (showTickTime.get()) {
            renderer.text(line("Tick: %.1f ms", monitor.tickTimeMs()), x + pad, ty, TEXT, shadow.get(), 1);
            ty += lineHeight;
        }
        if (showEntities.get()) {
            renderer.text(line("Entities: %d", monitor.entityCount()), x + pad, ty, TEXT, shadow.get(), 1);
            ty += lineHeight;
        }
        if (showPackets.get()) {
            renderer.text(line("In: %d/s", monitor.packetRateIn()), x + pad, ty, TEXT, shadow.get(), 1);
        }

        // Touch the diagnostics tracker so its spike history is primed for tooltips.
        DiagnosticsTracker.INSTANCE.sparkCount();
    }

    private String line(String format, Object... args) {
        builder.setLength(0);
        builder.append(String.format(format, args));
        return builder.toString();
    }
}
