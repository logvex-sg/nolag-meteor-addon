package com.antilagaddon.perf;

import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.block.entity.BlockEntityType;

/**
 * Block-entity categories that can be distance-suppressed on the client.
 *
 * <p>Only categories whose renderers are known to be expensive are exposed. Their block entities
 * continue to exist and function; only their rendering is skipped past the configured distance.</p>
 */
public enum BlockKind {
    Sign,
    Chest,
    Banner,
    Spawner,
    Misc;

    /** @return the category for a block-entity type, or Misc for uncategorised ones. */
    public static BlockKind of(BlockEntityType<?> type) {
        if (type == BlockEntityType.SIGN || type == BlockEntityType.HANGING_SIGN) return Sign;
        if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST
            || type == BlockEntityType.ENDER_CHEST || type == BlockEntityType.SHULKER_BOX
            || type == BlockEntityType.BARREL) return Chest;
        if (type == BlockEntityType.BANNER) return Banner;
        if (type == BlockEntityType.MOB_SPAWNER || type == BlockEntityType.TRIAL_SPAWNER) return Spawner;
        return Misc;
    }

    public boolean enabled() {
        Setting<Boolean> setting = switch (this) {
            case Sign -> AntiLagConfig.beSignEnabled;
            case Chest -> AntiLagConfig.beChestEnabled;
            case Banner -> AntiLagConfig.beBannerEnabled;
            case Spawner -> AntiLagConfig.beSpawnerEnabled;
            case Misc -> AntiLagConfig.beMiscEnabled;
        };
        return AntiLagConfig.get(setting);
    }

    public double distance() {
        Setting<Double> setting = switch (this) {
            case Sign -> AntiLagConfig.beSignDistance;
            case Chest -> AntiLagConfig.beChestDistance;
            case Banner -> AntiLagConfig.beBannerDistance;
            case Spawner -> AntiLagConfig.beSpawnerDistance;
            case Misc -> AntiLagConfig.beMiscDistance;
        };
        return AntiLagConfig.getDouble(setting);
    }
}
