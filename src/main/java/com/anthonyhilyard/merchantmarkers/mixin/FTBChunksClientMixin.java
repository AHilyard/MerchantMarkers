package com.anthonyhilyard.merchantmarkers.mixin;

import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.ftb.mods.ftbchunks.client.EntityIcons;
import dev.ftb.mods.ftbchunks.client.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.integration.MapIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

@Mixin(FTBChunksClient.class)
public class FTBChunksClientMixin
{
	private final static ResourceLocation VILLAGER_LOCATION = new ResourceLocation("villager");

	@Redirect(method = "mapIcons", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/client/EntityIcons;get(Lnet/minecraft/world/entity/Entity;)Ldev/ftb/mods/ftblibrary/icon/Icon;", remap = false), remap = false)
	public Icon redirectGet(Entity entity)
	{
		// If this is a villager, return the dynamic texture instead of the default one.
		if (MerchantMarkersConfig.getInstance().showOnMiniMap.get() &&  ForgeRegistries.ENTITIES.getKey(entity.getType()).equals(VILLAGER_LOCATION))
		{
			return Icon.getIcon(FTBChunksHandler.villagerTexture);
		}

		return EntityIcons.get(entity);
	}

	@Redirect(method = "mapIcons", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;getCategory()Lnet/minecraft/world/entity/MobCategory;"))
	public MobCategory redirectGetCategory(EntityType<?> entityType)
	{
		if (MerchantMarkersConfig.getInstance().showOnMiniMap.get())
		{
			// Redirect the getCategory call to tell FTB Chunks that villagers aren't "Misc" category (so they aren't skipped).
			if (ForgeRegistries.ENTITIES.getKey(entityType).equals(VILLAGER_LOCATION))
			{
				return MobCategory.CREATURE;
			}
		}

		return entityType.getCategory();
	}

	@Redirect(method = "renderHud", at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/integration/MapIcon;draw(Ldev/ftb/mods/ftbchunks/client/MapType;Lcom/mojang/blaze3d/vertex/PoseStack;IIIIZ)V", remap = false), remap = false)
	public void redirectIconDraw(MapIcon icon, MapType mapType, PoseStack stack, int x, int y, int w, int h, boolean outsideVisibleArea)
	{
		if (icon instanceof EntityMapIcon entityIcon)
		{
			Entity entity = entityIcon.entity;

			// If this is a villager, return the dynamic texture instead of the default one.
			if (MerchantMarkersConfig.getInstance().showOnMiniMap.get() && ForgeRegistries.ENTITIES.getKey(entity.getType()).equals(VILLAGER_LOCATION))
			{
				FTBChunksHandler.setCurrentEntity(entity);
			}
		}

		icon.draw(mapType, stack, x, y, w, h, outsideVisibleArea);
	}
}