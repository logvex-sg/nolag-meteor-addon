package com.antilagaddon.mixin;

import com.antilagaddon.perf.Suppression;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels rendering of entities that AntiLag has decided to suppress.
 *
 * <p>Meteor does not expose a cancellable per-entity render event, so this single mixin is the
 * minimal hook required. It only skips client-side drawing; the entity remains in the world and no
 * packets are touched. The entity reference comes from Meteor's own {@code IEntityRenderState},
 * avoiding a duplicate mixin.</p>
 */
@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void antilag$skipSuppressed(EntityRenderState renderState, CameraRenderState cameraState,
                                        double x, double y, double z, MatrixStack matrices,
                                        OrderedRenderCommandQueue queue, CallbackInfo info) {
        Entity entity = ((IEntityRenderState) renderState).meteor$getEntity();
        if (entity != null && Suppression.shouldSuppressEntity(entity)) info.cancel();
    }
}
