package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity>
{
	@SuppressWarnings("unchecked")
	@Inject(method = "render", at = @At(value  = "HEAD"))
	public void render(T entity, float f, float g, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo info)
	{
		if (MerchantMarkers.showMarkers.isDown() || MerchantMarkersConfig.INSTANCE.alwaysShow.get())
		{
			// Try rendering markers now, before we render the nameplates.
			Markers.renderMarker((EntityRenderer<T>)(Object)this, entity, entity.getDisplayName(), poseStack, buffer, packedLight);
		}
	}
}