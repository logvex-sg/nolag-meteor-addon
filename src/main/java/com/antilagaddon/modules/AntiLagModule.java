package com.antilagaddon.modules;

import com.antilagaddon.AntiLagAddon;
import com.antilagaddon.perf.AdaptiveController;
import com.antilagaddon.perf.AntiLagConfig;
import com.antilagaddon.perf.BlockEntityFilter;
import com.antilagaddon.perf.ParticleFilter;
import com.antilagaddon.perf.PerformanceMonitor;
import com.antilagaddon.perf.SuppressTickController;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/**
 * Main AntiLag module: the configurable, master set of client-side rendering protections.
 *
 * <p>All settings live here and are shared (through {@link AntiLagConfig}) by the EntityLimiter and
 * ParticleLimiter modules so that there is a single source of truth. Enabling this module turns the
 * protections on; disabling it restores normal rendering by making every check short-circuit.</p>
 */
public class AntiLagModule extends Module {
    private final SettingGroup sgEntity = settings.createGroup("Entity Suppression");
    private final SettingGroup sgParticle = settings.createGroup("Particle Suppression");
    private final SettingGroup sgBlockEntity = settings.createGroup("Block Entity Rendering");
    private final SettingGroup sgAdaptive = settings.createGroup("Adaptive Performance");
    private final SettingGroup sgDiagnostics = settings.createGroup("Diagnostics");

    // ---- entity ----------------------------------------------------------

    private final Setting<Boolean> entitiesEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("entities")
        .description("Master switch for entity rendering suppression.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> itemsEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("items")
        .description("Suppress dropped item entities beyond the item distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> xpOrbsEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("experience-orbs")
        .description("Suppress experience orb entities beyond the XP orb distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> projectilesEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("projectiles")
        .description("Suppress projectile entities (arrows, snowballs, etc.) beyond the projectile distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> fallingBlocksEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("falling-blocks")
        .description("Suppress falling block entities beyond the falling block distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> armorStandsEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("armor-stands")
        .description("Suppress armor stand entities beyond the armor stand distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> vehiclesEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("vehicles")
        .description("Suppress vehicles (boats, minecarts) beyond the vehicle distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> displaysEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("displays")
        .description("Suppress display entities (item/block/text displays) beyond the display distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> miscEntitiesEnabled = sgEntity.add(new BoolSetting.Builder()
        .name("misc-entities")
        .description("Suppress other non-living entities (TNT, end crystals, decorations, etc.).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> itemDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("item-distance")
        .description("Maximum distance (blocks) at which item entities render. Set to 0 to never hide by distance.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> xpOrbDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("experience-orb-distance")
        .description("Maximum distance (blocks) at which experience orbs render.")
        .defaultValue(24)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> projectileDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("projectile-distance")
        .description("Maximum distance (blocks) at which projectiles render.")
        .defaultValue(48)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> fallingBlockDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("falling-block-distance")
        .description("Maximum distance (blocks) at which falling blocks render.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> armorStandDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("armor-stand-distance")
        .description("Maximum distance (blocks) at which armor stands render.")
        .defaultValue(24)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> vehicleDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("vehicle-distance")
        .description("Maximum distance (blocks) at which vehicles render.")
        .defaultValue(48)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> displayDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("display-distance")
        .description("Maximum distance (blocks) at which display entities render.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> miscEntityDistance = sgEntity.add(new DoubleSetting.Builder()
        .name("misc-entity-distance")
        .description("Maximum distance (blocks) at which other non-living entities render.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Integer> entityBudget = sgEntity.add(new IntSetting.Builder()
        .name("entity-budget")
        .description("Maximum number of suppressible entities to render near you; the rest are hidden. 0 disables the budget.")
        .defaultValue(0)
        .min(0)
        .sliderMax(200)
        .build()
    );

    // ---- particles -------------------------------------------------------

    private final Setting<Boolean> particlesEnabled = sgParticle.add(new BoolSetting.Builder()
        .name("particles")
        .description("Master switch for particle suppression.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> particleExplosion = sgParticle.add(new BoolSetting.Builder()
        .name("explosion")
        .description("Suppress explosion particles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> particleSmoke = sgParticle.add(new BoolSetting.Builder()
        .name("smoke")
        .description("Suppress smoke and cloud particles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> particleFlame = sgParticle.add(new BoolSetting.Builder()
        .name("flame")
        .description("Suppress flame and soul-fire particles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> particlePotion = sgParticle.add(new BoolSetting.Builder()
        .name("potion")
        .description("Suppress potion effect particles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> particleBlockDestruction = sgParticle.add(new BoolSetting.Builder()
        .name("block-destruction")
        .description("Suppress block break and block dust particles.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> particleOther = sgParticle.add(new BoolSetting.Builder()
        .name("other")
        .description("Suppress all other particle types not covered above.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> particleBudget = sgParticle.add(new IntSetting.Builder()
        .name("particle-budget")
        .description("Maximum number of particles spawned per client tick; further particles are dropped. 0 disables the budget.")
        .defaultValue(0)
        .min(0)
        .sliderMax(2000)
        .build()
    );

    // ---- block entities --------------------------------------------------

    private final Setting<Boolean> blockEntitiesEnabled = sgBlockEntity.add(new BoolSetting.Builder()
        .name("block-entities")
        .description("Master switch for block-entity rendering suppression.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> beSignEnabled = sgBlockEntity.add(new BoolSetting.Builder()
        .name("signs")
        .description("Suppress sign and hanging sign rendering beyond the sign distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> beChestEnabled = sgBlockEntity.add(new BoolSetting.Builder()
        .name("chests")
        .description("Suppress chest, barrel, ender chest and shulker rendering beyond the chest distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> beBannerEnabled = sgBlockEntity.add(new BoolSetting.Builder()
        .name("banners")
        .description("Suppress banner rendering beyond the banner distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> beSpawnerEnabled = sgBlockEntity.add(new BoolSetting.Builder()
        .name("spawners")
        .description("Suppress mob spawner rendering beyond the spawner distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> beMiscEnabled = sgBlockEntity.add(new BoolSetting.Builder()
        .name("misc-block-entities")
        .description("Suppress other block entities beyond the misc distance.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> beSignDistance = sgBlockEntity.add(new DoubleSetting.Builder()
        .name("sign-distance")
        .description("Maximum distance (blocks) at which signs render.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> beChestDistance = sgBlockEntity.add(new DoubleSetting.Builder()
        .name("chest-distance")
        .description("Maximum distance (blocks) at which chests render.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> beBannerDistance = sgBlockEntity.add(new DoubleSetting.Builder()
        .name("banner-distance")
        .description("Maximum distance (blocks) at which banners render.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> beSpawnerDistance = sgBlockEntity.add(new DoubleSetting.Builder()
        .name("spawner-distance")
        .description("Maximum distance (blocks) at which spawners render.")
        .defaultValue(24)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<Double> beMiscDistance = sgBlockEntity.add(new DoubleSetting.Builder()
        .name("misc-block-entity-distance")
        .description("Maximum distance (blocks) at which other block entities render.")
        .defaultValue(32)
        .min(0)
        .sliderMax(128)
        .build()
    );

    // ---- adaptive --------------------------------------------------------

    private final Setting<Boolean> adaptiveEnabled = sgAdaptive.add(new BoolSetting.Builder()
        .name("adaptive")
        .description("Automatically increase suppression when FPS drops and restore quality when it recovers.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> normalFps = sgAdaptive.add(new DoubleSetting.Builder()
        .name("normal-fps")
        .description("FPS at or above which the Normal level is restored.")
        .defaultValue(90)
        .min(1)
        .sliderMax(300)
        .build()
    );

    private final Setting<Double> balancedFps = sgAdaptive.add(new DoubleSetting.Builder()
        .name("balanced-fps")
        .description("FPS below which the Balanced level is applied.")
        .defaultValue(60)
        .min(1)
        .sliderMax(300)
        .build()
    );

    private final Setting<Double> performanceFps = sgAdaptive.add(new DoubleSetting.Builder()
        .name("performance-fps")
        .description("FPS below which the Performance level is applied.")
        .defaultValue(40)
        .min(1)
        .sliderMax(300)
        .build()
    );

    private final Setting<Double> emergencyFps = sgAdaptive.add(new DoubleSetting.Builder()
        .name("emergency-fps")
        .description("FPS below which the Emergency level is applied.")
        .defaultValue(20)
        .min(1)
        .sliderMax(300)
        .build()
    );

    private final Setting<Integer> upgradeCooldown = sgAdaptive.add(new IntSetting.Builder()
        .name("upgrade-cooldown")
        .description("Consecutive ticks the degraded condition must persist before suppression increases.")
        .defaultValue(40)
        .min(1)
        .sliderMax(400)
        .build()
    );

    private final Setting<Integer> downgradeCooldown = sgAdaptive.add(new IntSetting.Builder()
        .name("downgrade-cooldown")
        .description("Consecutive ticks of recovery before suppression is eased back.")
        .defaultValue(200)
        .min(1)
        .sliderMax(1200)
        .build()
    );

    private final Setting<Boolean> adaptiveDebug = sgAdaptive.add(new BoolSetting.Builder()
        .name("adaptive-debug")
        .description("Log adaptive level changes to chat.")
        .defaultValue(false)
        .build()
    );

    // ---- diagnostics -----------------------------------------------------

    private final Setting<Integer> updateScanInterval = sgDiagnostics.add(new IntSetting.Builder()
        .name("scan-interval")
        .description("Ticks between entity-budget rebuild scans. Higher values reduce overhead.")
        .defaultValue(10)
        .min(1)
        .sliderMax(120)
        .build()
    );

    private final Setting<Boolean> debugLog = sgDiagnostics.add(new BoolSetting.Builder()
        .name("debug-log")
        .description("Log diagnostic warnings to chat.")
        .defaultValue(false)
        .build()
    );

    private final SuppressTickController controller = new SuppressTickController();
    private boolean monitorSubscribed;
    private boolean filtersSubscribed;

    public AntiLagModule() {
        super(AntiLagAddon.CATEGORY, "anti-lag", "Client-side protections against lag machines, entity, particle and block-entity spam.");

        // Publish the shared settings for the limiter modules.
        AntiLagConfig.entitiesEnabled = entitiesEnabled;
        AntiLagConfig.itemsEnabled = itemsEnabled;
        AntiLagConfig.xpOrbsEnabled = xpOrbsEnabled;
        AntiLagConfig.projectilesEnabled = projectilesEnabled;
        AntiLagConfig.fallingBlocksEnabled = fallingBlocksEnabled;
        AntiLagConfig.armorStandsEnabled = armorStandsEnabled;
        AntiLagConfig.vehiclesEnabled = vehiclesEnabled;
        AntiLagConfig.displaysEnabled = displaysEnabled;
        AntiLagConfig.miscEntitiesEnabled = miscEntitiesEnabled;

        AntiLagConfig.itemDistance = itemDistance;
        AntiLagConfig.xpOrbDistance = xpOrbDistance;
        AntiLagConfig.projectileDistance = projectileDistance;
        AntiLagConfig.fallingBlockDistance = fallingBlockDistance;
        AntiLagConfig.armorStandDistance = armorStandDistance;
        AntiLagConfig.vehicleDistance = vehicleDistance;
        AntiLagConfig.displayDistance = displayDistance;
        AntiLagConfig.miscEntityDistance = miscEntityDistance;

        AntiLagConfig.entityBudget = entityBudget;
        AntiLagConfig.updateScanInterval = updateScanInterval;

        AntiLagConfig.particlesEnabled = particlesEnabled;
        AntiLagConfig.particleExplosion = particleExplosion;
        AntiLagConfig.particleSmoke = particleSmoke;
        AntiLagConfig.particleFlame = particleFlame;
        AntiLagConfig.particlePotion = particlePotion;
        AntiLagConfig.particleBlockDestruction = particleBlockDestruction;
        AntiLagConfig.particleOther = particleOther;
        AntiLagConfig.particleBudget = particleBudget;

        AntiLagConfig.blockEntitiesEnabled = blockEntitiesEnabled;
        AntiLagConfig.beSignEnabled = beSignEnabled;
        AntiLagConfig.beChestEnabled = beChestEnabled;
        AntiLagConfig.beBannerEnabled = beBannerEnabled;
        AntiLagConfig.beSpawnerEnabled = beSpawnerEnabled;
        AntiLagConfig.beMiscEnabled = beMiscEnabled;
        AntiLagConfig.beSignDistance = beSignDistance;
        AntiLagConfig.beChestDistance = beChestDistance;
        AntiLagConfig.beBannerDistance = beBannerDistance;
        AntiLagConfig.beSpawnerDistance = beSpawnerDistance;
        AntiLagConfig.beMiscDistance = beMiscDistance;

        AntiLagConfig.adaptiveEnabled = adaptiveEnabled;
        AntiLagConfig.normalFps = normalFps;
        AntiLagConfig.balancedFps = balancedFps;
        AntiLagConfig.performanceFps = performanceFps;
        AntiLagConfig.emergencyFps = emergencyFps;
        AntiLagConfig.upgradeCooldown = upgradeCooldown;
        AntiLagConfig.downgradeCooldown = downgradeCooldown;
        AntiLagConfig.adaptiveDebug = adaptiveDebug;
        AntiLagConfig.debugLog = debugLog;
    }

    @Override
    public void onActivate() {
        AntiLagConfig.masterEnabled = true;
        controller.reset();
        subscribeMonitor();

        if (!filtersSubscribed) {
            ParticleFilter.INSTANCE.addConsumer();
            BlockEntityFilter.INSTANCE.addConsumer();
            filtersSubscribed = true;
        }
    }

    @Override
    public void onDeactivate() {
        // Disabling instantly restores normal rendering: every suppression check short-circuits.
        AntiLagConfig.masterEnabled = false;
        unsubscribeMonitor();

        if (filtersSubscribed) {
            ParticleFilter.INSTANCE.removeConsumer();
            BlockEntityFilter.INSTANCE.removeConsumer();
            filtersSubscribed = false;
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (!controller.inWorld()) return;
        controller.beginTick();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!controller.inWorld()) return;
        controller.tick();
    }

    private void subscribeMonitor() {
        if (monitorSubscribed) return;
        meteordevelopment.meteorclient.MeteorClient.EVENT_BUS.subscribe(PerformanceMonitor.INSTANCE);
        monitorSubscribed = true;
    }

    private void unsubscribeMonitor() {
        if (!monitorSubscribed) return;
        meteordevelopment.meteorclient.MeteorClient.EVENT_BUS.unsubscribe(PerformanceMonitor.INSTANCE);
        monitorSubscribed = false;
    }

    @Override
    public String getInfoString() {
        if (!AdaptiveController.INSTANCE.active()) return null;
        return AdaptiveController.INSTANCE.level().title();
    }
}
