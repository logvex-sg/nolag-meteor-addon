package com.antilagaddon.modules;

import com.antilagaddon.AntiLagAddon;
import com.antilagaddon.perf.AntiLagConfig;
import com.antilagaddon.perf.LimiterManager;
import com.antilagaddon.perf.PerformanceMonitor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/**
 * Standalone entity rendering limiter.
 *
 * <p>Reuses the shared entity distance/budget settings from the AntiLag module (so the two never
 * disagree) but drives its own enable flag and scan cadence. When the AntiLag module is disabled
 * this module can still apply entity suppression on its own.</p>
 */
public class EntityLimiter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> useDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("distance-limit")
        .description("Hide suppressible entities beyond their configured category distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> useBudget = sgGeneral.add(new BoolSetting.Builder()
        .name("entity-budget")
        .description("Hide the furthest suppressible entities once the entity budget is reached.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> scanInterval = sgGeneral.add(new IntSetting.Builder()
        .name("scan-interval")
        .description("Ticks between entity-budget rebuild scans.")
        .defaultValue(10)
        .min(1)
        .sliderMax(120)
        .build()
    );

    private int tickCounter;

    public EntityLimiter() {
        super(AntiLagAddon.CATEGORY, "entity-limiter", "Category-specific entity rendering suppression with distance controls and a render budget.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        AntiLagConfig.entityLimiterActive = true;
        AntiLagConfig.entityLimiterUseDistance = useDistance;
        AntiLagConfig.entityLimiterUseBudget = useBudget;
    }

    @Override
    public void onDeactivate() {
        AntiLagConfig.entityLimiterActive = false;
        LimiterManager.INSTANCE.rebuildEntityBudget(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        var mc = meteordevelopment.meteorclient.MeteorClient.mc;
        if (mc.world == null || mc.player == null) return;

        PerformanceMonitor.INSTANCE.sampleFrame();

        int interval = Math.max(1, useBudget.get() ? scanInterval.get() : AntiLagConfig.getInt(AntiLagConfig.updateScanInterval));
        tickCounter++;
        if (tickCounter < interval) return;
        tickCounter = 0;

        boolean applyBudget = useBudget.get() && AntiLagConfig.getInt(AntiLagConfig.entityBudget) > 0;
        if (applyBudget || useDistance.get()) LimiterManager.INSTANCE.rebuildEntityBudget(applyBudget);
    }

    /** @return true when this module currently hides entities by distance. */
    public boolean hidesByDistance() {
        return isActive() && useDistance.get();
    }

    /** @return true when this module currently hides entities by budget. */
    public boolean hidesByBudget() {
        return isActive() && useBudget.get();
    }
}
