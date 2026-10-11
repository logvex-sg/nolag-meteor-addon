package com.antilagaddon.perf;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.orbit.EventHandler;

/**
 * Subscribes to Meteor's {@link ParticleEvent} and cancels spawning of suppressed particles.
 *
 * <p>Meteor already posts this event for every particle, so no mixin is required. Registration is
 * reference-counted so the main AntiLag module and the ParticleLimiter module can both rely on it
 * without double-subscribing.</p>
 */
public final class ParticleFilter {
    public static final ParticleFilter INSTANCE = new ParticleFilter();

    private int consumers;

    private ParticleFilter() {}

    public synchronized void addConsumer() {
        if (consumers++ == 0) MeteorClient.EVENT_BUS.subscribe(this);
    }

    public synchronized void removeConsumer() {
        if (consumers > 0 && --consumers == 0) MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    @EventHandler
    private void onParticle(ParticleEvent event) {
        if (Suppression.shouldSuppressParticle(event.particle)) event.cancel();
    }
}
