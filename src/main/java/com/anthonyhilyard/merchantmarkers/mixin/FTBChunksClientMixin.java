package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler;
import com.anthonyhilyard.merchantmarkers.render.Markers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.ftb.mods.ftbchunks.client.mapicon.EntityIcons;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

@Mixin(FTBChunksClient.class)
public class FTBChunksClientMixin
{
	private final static ResourceLocation VILLAGER_LOCATION = new ResourceLocation("villager");

	@Redirect(method = "mapIcons", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/client/mapicon/EntityIcons;get(Lnet/minecraft/world/entity/Entity;)Ldev/ftb/mods/ftblibrary/icon/Icon;"), require = 0)
	public Icon redirectGet(Entity entity)
	{
		// If this is a villager, return the dynamic texture instead of the default one.
		if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get() && Markers.shouldShowMarker(entity))
		{
			return Icon.getIcon(FTBChunksHandler.villagerTexture);
		}

		return EntityIcons.get(entity);
	}

	@Redirect(method = "mapIcons", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;getCategory()Lnet/minecraft/world/entity/MobCategory;"), require = 0)
	public MobCategory redirectGetCategory(EntityType<?> entityType)
	{
		if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get())
		{
			// Redirect the getCategory call to tell FTB Chunks that villagers aren't "Misc" category (so they aren't skipped).
			if (BuiltInRegistries.ENTITY_TYPE.getKey(entityType).equals(VILLAGER_LOCATION))
			{
				return MobCategory.CREATURE;
			}
		}

		return entityType.getCategory();
	}

	@Redirect(method = "renderHud", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/api/client/icon/MapIcon;draw(Ldev/ftb/mods/ftbchunks/api/client/icon/MapType;Lnet/minecraft/client/gui/GuiGraphics;IIIIZI)V"), require = 0)
	public void redirectIconDraw(MapIcon icon, MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha)
	{
		if (icon instanceof EntityMapIcon entityIcon)
		{
			Entity entity = FTBChunksHandler.getEntityFromIcon(entityIcon);

			// If this is a villager, return the dynamic texture instead of the default one.
			if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get() && Markers.shouldShowMarker(entity))
			{
				FTBChunksHandler.setCurrentEntity(entity);
			}
		}

		icon.draw(mapType, graphics, x, y, w, h, outsideVisibleArea, iconAlpha);
	}
}