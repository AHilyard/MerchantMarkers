package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler;
import com.mojang.blaze3d.matrix.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.ftb.mods.ftbchunks.client.EntityIcons;
import dev.ftb.mods.ftbchunks.client.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.integration.MapIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;

@Mixin(FTBChunksClient.class)
public class FTBChunksClientMixin
{
	@Redirect(method = "mapIcons", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/client/EntityIcons;get(Lnet/minecraft/entity/Entity;)Ldev/ftb/mods/ftblibrary/icon/Icon;", remap = false), remap = false)
	public Icon redirectGet(Entity entity)
	{
		// If this is a villager, return the dynamic texture instead of the default one.
		if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get() && entity.getType().getRegistryName().equals(new ResourceLocation("villager")))
		{
			return Icon.getIcon(FTBChunksHandler.villagerTexture);
		}

		return EntityIcons.get(entity);
	}

	@Redirect(method = "mapIcons", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;getCategory()Lnet/minecraft/entity/EntityClassification;"))
	public EntityClassification redirectGetCategory(EntityType<?> entityType)
	{
		if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get())
		{
			// Redirect the getCategory call to tell FTB Chunks that villagers aren't "Misc" category (so they aren't skipped).
			if (entityType.getRegistryName().equals(new ResourceLocation("villager")))
			{
				return EntityClassification.CREATURE;
			}
		}

		return entityType.getCategory();
	}

	@Redirect(method = "renderHud", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/integration/MapIcon;draw(Ldev/ftb/mods/ftbchunks/client/MapType;Lcom/mojang/blaze3d/matrix/MatrixStack;IIIIZ)V", remap = false), remap = false)
	public void redirectIconDraw(MapIcon icon, MapType mapType, MatrixStack stack, int x, int y, int w, int h, boolean outsideVisibleArea)
	{
		if (icon instanceof EntityMapIcon)
		{
			EntityMapIcon entityIcon = (EntityMapIcon)icon;
			Entity entity = entityIcon.entity;
			// If this is a villager, return the dynamic texture instead of the default one.
			if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get() && entity.getType().getRegistryName().equals(new ResourceLocation("villager")))
			{
				FTBChunksHandler.setCurrentEntity(entity);
			}
		}
		icon.draw(mapType, stack, x, y, w, h, outsideVisibleArea);
	}
}
