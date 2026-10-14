package com.antilagaddon.perf;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Central decision logic for what may be suppressed on the client.
 *
 * <p>Nothing here removes entities, block entities or particles from the world; it only answers
 * whether the client should skip the expensive <em>rendering</em> (or particle spawning) of an
 * object. Gameplay-critical objects (the local player and other players) are never suppressed.</p>
 */
public final class Suppression {
    private Suppression() {}

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ---- entity helpers --------------------------------------------------

    /** @return the independent per-category enable flag, or null if the entity is uncategorised. */
    private static Boolean categoryEnabled(Entity entity) {
        if (entity instanceof PlayerEntity || entity.getType() == EntityType.PLAYER) return null;

        if (entity instanceof ItemEntity) return AntiLagConfig.get(AntiLagConfig.itemsEnabled);
        if (entity instanceof ExperienceOrbEntity) return AntiLagConfig.get(AntiLagConfig.xpOrbsEnabled);
        if (entity instanceof ProjectileEntity) return AntiLagConfig.get(AntiLagConfig.projectilesEnabled);
        if (entity instanceof FallingBlockEntity) return AntiLagConfig.get(AntiLagConfig.fallingBlocksEnabled);
        if (entity instanceof ArmorStandEntity) return AntiLagConfig.get(AntiLagConfig.armorStandsEnabled);
        if (entity instanceof VehicleEntity) return AntiLagConfig.get(AntiLagConfig.vehiclesEnabled);
        if (entity instanceof DisplayEntity) return AntiLagConfig.get(AntiLagConfig.displaysEnabled);

        // Remaining non-living entities (TNT, end crystals, decorations, ...) fall under "misc".
        if (!(entity instanceof LivingEntity)) return AntiLagConfig.get(AntiLagConfig.miscEntitiesEnabled);
        return null;
    }

    private static double categoryDistance(Entity entity) {
        if (entity instanceof ItemEntity) return AntiLagConfig.getDouble(AntiLagConfig.itemDistance);
        if (entity instanceof ExperienceOrbEntity) return AntiLagConfig.getDouble(AntiLagConfig.xpOrbDistance);
        if (entity instanceof ProjectileEntity) return AntiLagConfig.getDouble(AntiLagConfig.projectileDistance);
        if (entity instanceof FallingBlockEntity) return AntiLagConfig.getDouble(AntiLagConfig.fallingBlockDistance);
        if (entity instanceof ArmorStandEntity) return AntiLagConfig.getDouble(AntiLagConfig.armorStandDistance);
        if (entity instanceof VehicleEntity) return AntiLagConfig.getDouble(AntiLagConfig.vehicleDistance);
        if (entity instanceof DisplayEntity) return AntiLagConfig.getDouble(AntiLagConfig.displayDistance);
        return AntiLagConfig.getDouble(AntiLagConfig.miscEntityDistance);
    }

    /** @return true when the entity belongs to a category this addon may suppress at all. */
    public static boolean isSuppressibleCategory(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof PlayerEntity || entity.getType() == EntityType.PLAYER) return false;
        return Boolean.TRUE.equals(categoryEnabled(entity));
    }

    /**
     * Decides whether an entity's rendering should be skipped this frame.
     *
     * @param entity the entity being considered for rendering
     * @return true to skip rendering this entity
     */
    public static boolean shouldSuppressEntity(Entity entity) {
        if (entity == null || entity == mc.player) return false;
        if (entity instanceof PlayerEntity || entity.getType() == EntityType.PLAYER) return false;

        Boolean enabled = categoryEnabled(entity);
        if (enabled == null || !enabled) return false;

        boolean main = AntiLagConfig.masterEnabled;
        boolean limiter = AntiLagConfig.entityLimiterActive;

        if (!main && !limiter) return false;

        boolean hideByDistance =
            (main && AntiLagConfig.get(AntiLagConfig.entitiesEnabled))
            || (limiter && AntiLagConfig.get(AntiLagConfig.entityLimiterUseDistance));
        boolean hideByBudget =
            (main && AntiLagConfig.get(AntiLagConfig.entitiesEnabled) && AntiLagConfig.getInt(AntiLagConfig.entityBudget) > 0)
            || (limiter && AntiLagConfig.get(AntiLagConfig.entityLimiterUseBudget));

        if (hideByDistance && beyondDistance(entity, categoryDistance(entity))) return true;
        return hideByBudget && LimiterManager.INSTANCE.isHiddenByBudget(entity.getId());
    }

    private static boolean beyondDistance(Entity entity, double distance) {
        if (distance <= 0) return false;

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        double dx = entity.getX() - cam.x;
        double dy = entity.getY() - cam.y;
        double dz = entity.getZ() - cam.z;
        double limit = distance * AdaptiveController.INSTANCE.distanceFactor();

        return (dx * dx + dy * dy + dz * dz) > (limit * limit);
    }

    // ---- particles -------------------------------------------------------

    /** @return true when a particle of this type should not be spawned on the client. */
    public static boolean shouldSuppressParticle(ParticleEffect particle) {
        if (particle == null) return false;

        boolean main = AntiLagConfig.masterEnabled && AntiLagConfig.get(AntiLagConfig.particlesEnabled);
        boolean limiter = AntiLagConfig.particleLimiterActive;
        if (!main && !limiter) return false;

        boolean byBudget = (main && AntiLagConfig.getInt(AntiLagConfig.particleBudget) > 0)
            || (limiter && AntiLagConfig.get(AntiLagConfig.particleLimiterUseBudget));
        boolean byCategory = (main)
            || (limiter && AntiLagConfig.get(AntiLagConfig.particleLimiterUseCategories));

        if (byBudget && LimiterManager.INSTANCE.countParticle()) return true;
        if (!byCategory) return false;

        ParticleType<?> type = particle.getType();
        if (isExplosion(type)) return AntiLagConfig.get(AntiLagConfig.particleExplosion);
        if (isSmoke(type)) return AntiLagConfig.get(AntiLagConfig.particleSmoke);
        if (isFlame(type)) return AntiLagConfig.get(AntiLagConfig.particleFlame);
        if (isPotion(type)) return AntiLagConfig.get(AntiLagConfig.particlePotion);
        if (isBlockDestruction(type)) return AntiLagConfig.get(AntiLagConfig.particleBlockDestruction);
        return AntiLagConfig.get(AntiLagConfig.particleOther);
    }

    private static boolean isExplosion(ParticleType<?> t) {
        return t == ParticleTypes.EXPLOSION || t == ParticleTypes.EXPLOSION_EMITTER;
    }

    private static boolean isSmoke(ParticleType<?> t) {
        return t == ParticleTypes.SMOKE || t == ParticleTypes.LARGE_SMOKE
            || t == ParticleTypes.CAMPFIRE_COSY_SMOKE || t == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE
            || t == ParticleTypes.WHITE_SMOKE || t == ParticleTypes.CLOUD;
    }

    private static boolean isFlame(ParticleType<?> t) {
        return t == ParticleTypes.FLAME || t == ParticleTypes.SMALL_FLAME
            || t == ParticleTypes.SOUL_FIRE_FLAME || t == ParticleTypes.SOUL;
    }

    private static boolean isPotion(ParticleType<?> t) {
        return t == ParticleTypes.EFFECT || t == ParticleTypes.ENTITY_EFFECT
            || t == ParticleTypes.INSTANT_EFFECT || t == ParticleTypes.WITCH;
    }

    private static boolean isBlockDestruction(ParticleType<?> t) {
        return t == ParticleTypes.BLOCK || t == ParticleTypes.DUST
            || t == ParticleTypes.DUST_PILLAR || t == ParticleTypes.DUST_PLUME;
    }

    // ---- block entities --------------------------------------------------

    /**
     * Decides whether a block entity's rendering should be skipped this frame.
     *
     * @param blockEntity the block entity being rendered
     * @return true to skip rendering this block entity
     */
    public static boolean shouldSuppressBlockEntity(BlockEntity blockEntity) {
        if (!AntiLagConfig.masterEnabled) return false;
        if (!AntiLagConfig.get(AntiLagConfig.blockEntitiesEnabled)) return false;
        if (blockEntity == null) return false;

        BlockPos pos = blockEntity.getPos();
        BlockEntityType<?> type = blockEntity.getType();
        BlockKind kind = BlockKind.of(type);
        if (kind == null || !kind.enabled()) return false;

        double distance = kind.distance();
        if (distance <= 0) return false;

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        double dx = pos.getX() + 0.5 - cam.x;
        double dy = pos.getY() + 0.5 - cam.y;
        double dz = pos.getZ() + 0.5 - cam.z;
        double limit = distance * AdaptiveController.INSTANCE.distanceFactor();

        return (dx * dx + dy * dy + dz * dz) > (limit * limit);
    }
}
