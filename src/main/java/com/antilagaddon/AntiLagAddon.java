package com.antilagaddon;

import com.antilagaddon.hud.LagMonitorHud;
import com.antilagaddon.modules.AntiLagModule;
import com.antilagaddon.modules.EntityLimiter;
import com.antilagaddon.modules.LagMonitor;
import com.antilagaddon.modules.ParticleLimiter;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

/**
 * Meteor Client addon entrypoint.
 *
 * <p>Registers the AntiLag category, the four modules and the diagnostics HUD element. The main
 * AntiLag module is created first because it owns the shared settings the other modules read.</p>
 */
public class AntiLagAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("AntiLag");
    public static final HudGroup HUD_GROUP = new HudGroup("AntiLag");

    @Override
    public void onInitialize() {
        LOG.info("Initializing AntiLag Addon");

        Modules.get().add(new AntiLagModule());
        Modules.get().add(new EntityLimiter());
        Modules.get().add(new ParticleLimiter());
        Modules.get().add(new LagMonitor());

        Hud.get().register(LagMonitorHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.antilagaddon";
    }

    @Override
    public String getWebsite() {
        return "https://github.com/logvex-sg/nolag-meteor-addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("logvex-sg", "nolag-meteor-addon", "main", null);
    }
}
