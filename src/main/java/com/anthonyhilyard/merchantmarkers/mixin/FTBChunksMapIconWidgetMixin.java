package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.FTBChunksHandler;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.ftb.mods.ftbchunks.client.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.MapIconWidget;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.integration.MapIcon;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

@Mixin(MapIconWidget.class)
public class FTBChunksMapIconWidgetMixin
{
	@Redirect(method = "draw", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/integration/MapIcon;draw(Ldev/ftb/mods/ftbchunks/client/MapType;Lcom/mojang/blaze3d/vertex/PoseStack;IIIIZ)V", remap = false), remap = false)
	public void redirectIconDraw(MapIcon icon, MapType mapType, PoseStack stack, int x, int y, int w, int h, boolean outsideVisibleArea)
	{
		if (icon instanceof EntityMapIcon entityIcon)
		{
			Entity entity = entityIcon.entity;

			// If this is a villager, return the dynamic texture instead of the default one.
			if (MerchantMarkersConfig.getInstance().showOnMiniMap.get() && entity.getType().getRegistryName().equals(new ResourceLocation("villager")))
			{
				FTBChunksHandler.setCurrentEntity(entity);
			}
		}

		icon.draw(mapType, stack, x, y, w, h, outsideVisibleArea);
	}
}