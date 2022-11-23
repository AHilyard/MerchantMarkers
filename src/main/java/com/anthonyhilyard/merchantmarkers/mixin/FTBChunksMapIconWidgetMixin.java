package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler;
import com.mojang.blaze3d.matrix.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.ftb.mods.ftbchunks.client.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.MapIconWidget;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.integration.MapIcon;
import net.minecraft.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.ResourceLocation;

@SuppressWarnings("null")
@Mixin(MapIconWidget.class)
public class FTBChunksMapIconWidgetMixin
{
	private final static ResourceLocation VILLAGER_LOCATION = new ResourceLocation("villager");

	@Redirect(method = "draw", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/integration/MapIcon;draw(Ldev/ftb/mods/ftbchunks/client/MapType;Lcom/mojang/blaze3d/matrix/MatrixStack;IIIIZ)V", remap = false), remap = false)
	public void redirectIconDraw(MapIcon icon, MapType mapType, MatrixStack stack, int x, int y, int w, int h, boolean outsideVisibleArea)
	{
		if (icon instanceof EntityMapIcon)
		{
			EntityMapIcon entityIcon = (EntityMapIcon)icon;
			Entity entity = entityIcon.entity;

			// If this is a villager, return the dynamic texture instead of the default one.
			if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get() && ForgeRegistries.ENTITIES.getKey(entity.getType()).equals(VILLAGER_LOCATION))
			{
				FTBChunksHandler.setCurrentEntity(entity);
			}
		}

		icon.draw(mapType, stack, x, y, w, h, outsideVisibleArea);
	}
}