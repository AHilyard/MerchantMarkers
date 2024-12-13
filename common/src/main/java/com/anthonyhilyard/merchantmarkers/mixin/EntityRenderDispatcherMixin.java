package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
{
	@Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
			at = @At(value  = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = Shift.BEFORE))
	private <E extends Entity, S extends EntityRenderState> void render(E entity, double x, double y, double z, float partialTick, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, EntityRenderer<? super E, S> entityRenderer, CallbackInfo info)
	{
		if (MerchantMarkers.showMarkers.isDown() || MerchantMarkersConfig.getInstance().alwaysShow.get())
		{
			// Try rendering markers now, before we render the nameplates.
			Markers.renderMarker(entityRenderer, entity, entity.getDisplayName(), poseStack, multiBufferSource, packedLight);
		}
	}
}