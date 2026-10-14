package com.antilagaddon.perf;

import meteordevelopment.meteorclient.settings.Setting;

/**
 * Shared configuration for the AntiLag addon.
 *
 * <p>Rather than relying on inter-module lookups (which are fragile while modules are still being
 * registered), every feature reads the same static settings. The settings are registered into the
 * settings UI by {@code AntiLagModule}; this class only holds the references.</p>
 */
public final class AntiLagConfig {
    private AntiLagConfig() {}

    /** Global master switch mirrored from the module state. */
    public static volatile boolean masterEnabled = true;

    // ---- AntiLag module settings (populated by AntiLagModule) ----
    public static Setting<Boolean> entitiesEnabled;
    public static Setting<Boolean> itemsEnabled;
    public static Setting<Boolean> xpOrbsEnabled;
    public static Setting<Boolean> projectilesEnabled;
    public static Setting<Boolean> fallingBlocksEnabled;
    public static Setting<Boolean> armorStandsEnabled;
    public static Setting<Boolean> vehiclesEnabled;
    public static Setting<Boolean> displaysEnabled;
    public static Setting<Boolean> miscEntitiesEnabled;

    public static Setting<Double> itemDistance;
    public static Setting<Double> xpOrbDistance;
    public static Setting<Double> projectileDistance;
    public static Setting<Double> fallingBlockDistance;
    public static Setting<Double> armorStandDistance;
    public static Setting<Double> vehicleDistance;
    public static Setting<Double> displayDistance;
    public static Setting<Double> miscEntityDistance;

    public static Setting<Integer> entityBudget;
    public static Setting<Integer> updateScanInterval;

    public static Setting<Boolean> particlesEnabled;
    public static Setting<Boolean> particleExplosion;
    public static Setting<Boolean> particleSmoke;
    public static Setting<Boolean> particleFlame;
    public static Setting<Boolean> particlePotion;
    public static Setting<Boolean> particleBlockDestruction;
    public static Setting<Boolean> particleOther;
    public static Setting<Integer> particleBudget;

    public static Setting<Boolean> blockEntitiesEnabled;
    public static Setting<Boolean> beSignEnabled;
    public static Setting<Boolean> beChestEnabled;
    public static Setting<Boolean> beBannerEnabled;
    public static Setting<Boolean> beSpawnerEnabled;
    public static Setting<Boolean> beMiscEnabled;
    public static Setting<Double> beSignDistance;
    public static Setting<Double> beChestDistance;
    public static Setting<Double> beBannerDistance;
    public static Setting<Double> beSpawnerDistance;
    public static Setting<Double> beMiscDistance;

    public static Setting<Boolean> adaptiveEnabled;
    public static Setting<Double> normalFps;
    public static Setting<Double> balancedFps;
    public static Setting<Double> performanceFps;
    public static Setting<Double> emergencyFps;
    public static Setting<Integer> upgradeCooldown;
    public static Setting<Integer> downgradeCooldown;
    public static Setting<Boolean> adaptiveDebug;

    public static Setting<Boolean> debugLog;

    // ---- Standalone limiter modules --------------------------------------
    public static volatile boolean entityLimiterActive;
    public static Setting<Boolean> entityLimiterUseDistance;
    public static Setting<Boolean> entityLimiterUseBudget;

    public static volatile boolean particleLimiterActive;
    public static Setting<Boolean> particleLimiterUseCategories;
    public static Setting<Boolean> particleLimiterUseBudget;

    public static boolean get(Setting<Boolean> s) { return s != null && s.get(); }
    public static double getDouble(Setting<Double> s) { return s == null ? 0 : s.get(); }
    public static int getInt(Setting<Integer> s) { return s == null ? 0 : s.get(); }
}
