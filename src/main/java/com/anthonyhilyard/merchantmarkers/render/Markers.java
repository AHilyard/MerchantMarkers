package com.anthonyhilyard.merchantmarkers.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class Markers
{
	public static record MarkerResource(ResourceLocation texture, boolean overlay) {}

	public static final ResourceLocation MARKER_ARROW = new ResourceLocation(Loader.MODID, "textures/entity/villager/arrow.png");
	public static final ResourceLocation ICON_OVERLAY = new ResourceLocation(Loader.MODID, "textures/entity/villager/overlay.png");
	public static final ResourceLocation DEFAULT_ICON = new ResourceLocation(Loader.MODID, "textures/entity/villager/default.png");

	private static Map<String, MarkerResource> resourceCache = new HashMap<>();

	public static String getIconName(Entity entity)
	{
		String iconName = "default";
		if (entity instanceof Villager)
		{
			iconName = ((Villager)entity).getVillagerData().getProfession().getName();
		}
		else if (entity instanceof WanderingTrader)
		{
			iconName = "wandering_trader";
		}
		else
		{
			iconName = entity.getClass().getName().toLowerCase();
		}
		return iconName;
	}

	public static void renderMarker(EntityRenderer<?> renderer, Entity entity, Component component, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		if (entity instanceof AbstractVillager)
		{
			Minecraft mc = Minecraft.getInstance();
			String iconName = getIconName(entity);
			
			// Skip professions in the blacklist.
			if (MerchantMarkersConfig.INSTANCE.professionBlacklist.get().contains(iconName))
			{
				return;
			}

			double squareDistance = renderer.entityRenderDispatcher.distanceToSqr(entity);
			double maxDistance = MerchantMarkersConfig.INSTANCE.maxDistance.get();
			
			// If this entity is too far away, don't render the markers.
			if (squareDistance > maxDistance * maxDistance)
			{
				return;
			}

			double fadePercent = MerchantMarkersConfig.INSTANCE.fadePercent.get();
			double currentAlpha = 1.0;
			
			// We won't do any calculations if fadePercent is 100, since that would make a division by zero.
			if (fadePercent < 100.0)
			{
				// Calculate the distance at which fading begins.
				double startFade = ((1.0 - (fadePercent / 100.0)) * maxDistance);

				// Calculate the current alpha value for this marker.
				currentAlpha = Mth.clamp(1.0 - ((Math.sqrt(squareDistance) - startFade) / (maxDistance - startFade)), 0.0, 1.0);
			}

			float f = entity.getBbHeight() + 0.5F;
			int i = "deadmau5".equals(component.getString()) ? -28 : -18;
			poseStack.pushPose();
			poseStack.translate(0.0D, (double)f, 0.0D);
			poseStack.mulPose(renderer.entityRenderDispatcher.cameraOrientation());
			poseStack.scale(-0.025F, -0.025F, 0.025F);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableDepthTest();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.3f * (float)currentAlpha);

			boolean showArrow = MerchantMarkersConfig.INSTANCE.showArrow.get();

			if (showArrow)
			{
				RenderSystem.setShaderTexture(0, MARKER_ARROW);
				GuiComponent.blit(poseStack, -8, i + 8, 0, 0, 16, 8, 16, 8);
			}

			renderMarker(getMarkerResource(mc, iconName), poseStack, -8, showArrow ? i - 9 : i);

			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float)currentAlpha);

			renderMarker(getMarkerResource(mc, iconName), poseStack, -8, showArrow ? i - 9 : i);

			if (showArrow)
			{
				RenderSystem.setShaderTexture(0, MARKER_ARROW);
				GuiComponent.blit(poseStack, -8, i + 8, 0, 0, 16, 8, 16, 8);
			}

			poseStack.popPose();
		}
	}

	public static void clearResourceCache()
	{
		resourceCache.clear();
	}

	@SuppressWarnings("deprecation")
	public static MarkerResource getMarkerResource(Minecraft mc, String iconName) //, Entity entity)
	{
		// Returned the cached value, if there is one.
		if (resourceCache.containsKey(iconName))
		{
			return resourceCache.get(iconName);
		}

		MarkerResource result = null;

		switch (MerchantMarkersConfig.MarkerType.fromText(MerchantMarkersConfig.INSTANCE.markerType.get()).get())
		{
			case ITEMS:
			{
				String associatedItemKey = MerchantMarkersConfig.INSTANCE.associatedItems.get().get(iconName);
				if (associatedItemKey != null)
				{
					Item associatedItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(associatedItemKey));

					ItemRenderer itemRenderer = mc.getItemRenderer();
					BakedModel bakedModel = itemRenderer.getModel(new ItemStack(associatedItem), (Level)null, mc.player, 0);

					TextureAtlasSprite sprite = bakedModel.getParticleIcon();
					ResourceLocation spriteLocation = new ResourceLocation(sprite.getName().getNamespace(), String.format("textures/%s%s", sprite.getName().getPath(), ".png"));
					result = new MarkerResource(spriteLocation, true);
				}
				break;
			}
			case JOBS:
			{
				// If the entity is a villager, find the (first) job block for their profession.
				VillagerProfession profession = Registry.VILLAGER_PROFESSION.get(new ResourceLocation(iconName));
				if (profession != VillagerProfession.NONE)
				{
					Set<BlockState> jobBlockStates = profession.getJobPoiType().getBlockStates();

					if (!jobBlockStates.isEmpty())
					{
						BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
						BakedModel bakedModel = blockRenderer.getBlockModel(jobBlockStates.iterator().next());

						TextureAtlasSprite sprite = bakedModel.getParticleIcon();
						ResourceLocation spriteLocation = new ResourceLocation(sprite.getName().getNamespace(), String.format("textures/%s%s", sprite.getName().getPath(), ".png"));
						result = new MarkerResource(spriteLocation, true);
					}
				}
				break;
			}
			case CUSTOM:
			default:
			{
				// Check if the given resource exists, otherwise use the default icon.
				ResourceLocation iconResource = new ResourceLocation(Loader.MODID, String.format("textures/entity/villager/markers/%s.png", iconName));
				if (mc.getResourceManager().hasResource(iconResource))
				{
					result = new MarkerResource(iconResource, true);
				}
				break;
			}
			// Render the generic icon for everything by falling through.
			case GENERIC:
			break;
		}

		if (result == null)
		{
			// If we got this far, we were missing something so just render the default icon.
			result = new MarkerResource(DEFAULT_ICON, false);
		}

		// Cache the result.
		resourceCache.put(iconName, result);
		return result;
	}

	private static void renderMarker(MarkerResource resource, PoseStack poseStack, int x, int y)
	{
		renderIcon(resource.texture, poseStack, x, y);
		if (resource.overlay)
		{
			int overlayIndex = MerchantMarkersConfig.INSTANCE.overlayIndex.get();
			if (overlayIndex == -1)
			{
				return;
			}

			poseStack.pushPose();
			poseStack.translate(0, 0, -1);
			renderIcon(ICON_OVERLAY, poseStack, x + 8, y + 8, 8, 8, (overlayIndex % 2) * 0.5f, (overlayIndex % 2) * 0.5f + 0.5f, (overlayIndex / 2) * 0.5f, (overlayIndex / 2) * 0.5f + 0.5f);
			poseStack.popPose();
		}
	}

	private static void renderIcon(ResourceLocation icon, PoseStack poseStack, int x, int y)
	{
		renderIcon(icon, poseStack, x, y, 16, 16, 0, 1, 0, 1);
	}

	private static void renderIcon(ResourceLocation icon, PoseStack poseStack, int x, int y, int w, int h, float u0, float u1, float v0, float v1)
	{
		Matrix4f matrix = poseStack.last().pose();

		Minecraft.getInstance().getTextureManager().getTexture(icon).setFilter(false, false);
		RenderSystem.setShaderTexture(0, icon);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(matrix, (float)x,			(float)(y + h),	0).uv(u0, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)(y + h),	0).uv(u1, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)y,			0).uv(u1, v0).endVertex();
		bufferbuilder.vertex(matrix, (float)x,			(float)y,			0).uv(u0, v0).endVertex();
		bufferbuilder.end();
		BufferUploader.end(bufferbuilder);
	}
}
