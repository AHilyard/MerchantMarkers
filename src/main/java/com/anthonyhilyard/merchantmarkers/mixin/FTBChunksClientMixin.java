package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.FTBChunksHandler;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.ftb.mods.ftbchunks.client.EntityIcons;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;

@Mixin(FTBChunksClient.class)
public class FTBChunksClientMixin
{
	@Redirect(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getType()Lnet/minecraft/entity/EntityType;"))
	public EntityType<?> redirectGetType(Entity entity)
	{
		if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get())
		{
			// Redirect the getType calls in renderHud so we can update the rendered villager texture on the fly.
			FTBChunksHandler.setCurrentEntity(entity);

			// Ensure the texture will be available for rendering.
			if (entity.getType().getRegistryName().equals(new ResourceLocation("villager")) &&
				!EntityIcons.ENTITY_ICONS.containsKey(entity.getType()))
			{
				EntityIcons.ENTITY_ICONS.put(entity.getType(), FTBChunksHandler.villagerTexture);
			}
		}

		return entity.getType();
	}

	@Redirect(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;getCategory()Lnet/minecraft/entity/EntityClassification;"))
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
}
