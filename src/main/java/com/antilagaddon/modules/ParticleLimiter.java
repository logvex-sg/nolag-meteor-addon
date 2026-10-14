package com.antilagaddon.modules;

import com.antilagaddon.AntiLagAddon;
import com.antilagaddon.perf.AntiLagConfig;
import com.antilagaddon.perf.LimiterManager;
import com.antilagaddon.perf.ParticleFilter;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/**
 * Standalone particle limiter.
 *
 * <p>Shares the particle category toggles and per-tick budget with the AntiLag module so the two
 * never conflict, but can be enabled on its own. The actual filtering happens centrally in
 * {@code Suppression.shouldSuppressParticle}, driven by Meteor's {@code ParticleEvent}.</p>
 */
public class ParticleLimiter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> useCategories = sgGeneral.add(new BoolSetting.Builder()
        .name("categories")
        .description("Apply the particle category suppression configured in AntiLag.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useBudget = sgGeneral.add(new BoolSetting.Builder()
        .name("particle-budget")
        .description("Apply the per-tick particle budget configured in AntiLag.")
        .defaultValue(false)
        .build()
    );

    public ParticleLimiter() {
        super(AntiLagAddon.CATEGORY, "particle-limiter", "Particle category suppression and visual-effect limits.");

        AntiLagConfig.particleLimiterUseCategories = useCategories;
        AntiLagConfig.particleLimiterUseBudget = useBudget;
    }

    @Override
    public void onActivate() {
        AntiLagConfig.particleLimiterActive = true;
        LimiterManager.INSTANCE.beginTick();
        ParticleFilter.INSTANCE.addConsumer();
    }

    @Override
    public void onDeactivate() {
        AntiLagConfig.particleLimiterActive = false;
        ParticleFilter.INSTANCE.removeConsumer();
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // When only this module runs, it owns the per-tick particle counter reset.
        if (!AntiLagConfig.masterEnabled) LimiterManager.INSTANCE.beginTick();
    }

    @Override
    public String getInfoString() {
        if (!isActive()) return null;
        return LimiterManager.INSTANCE.particleCountThisTick() + "/t";
    }
}
