package com.anthonyhilyard.merchantmarkers.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig.OverlayType;
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

public class Markers
{
	public static record MarkerResource(ResourceLocation texture, OverlayType overlay, int level) {}

	public static final ResourceLocation MARKER_ARROW = new ResourceLocation(Loader.MODID, "textures/entity/villager/arrow.png");
	public static final ResourceLocation ICON_OVERLAY = new ResourceLocation(Loader.MODID, "textures/entity/villager/overlay.png");
	public static final ResourceLocation NUMBER_OVERLAY = new ResourceLocation(Loader.MODID, "textures/entity/villager/numbers.png");
	public static final ResourceLocation DEFAULT_ICON = new ResourceLocation(Loader.MODID, "textures/entity/villager/default.png");

	private static Map<String, MarkerResource> resourceCache = new HashMap<>();

	public static String getProfessionName(Entity entity)
	{
		String iconName = "default";
		if (entity instanceof Villager)
		{
			// If the profession name contains any colons, replace them with double underscores.
			iconName = ((Villager)entity).getVillagerData().getProfession().toString().replace(":","__");
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

	public static int getProfessionLevel(Entity entity)
	{
		int level = 0;
		if (MerchantMarkersConfig.INSTANCE.showLevels() && entity instanceof Villager)
		{
			level = ((Villager)entity).getVillagerData().getLevel();
		}
		return level;
	}

	public static void renderMarker(EntityRenderer<?> renderer, Entity entity, Component component, PoseStack poseStack, MultiBufferSource buffer, int packedLight)
	{
		if (entity instanceof AbstractVillager)
		{
			Minecraft mc = Minecraft.getInstance();
			String profession = getProfessionName(entity);
			int level = getProfessionLevel(entity);
			
			// Skip professions in the blacklist.
			if (MerchantMarkersConfig.INSTANCE.professionBlacklist.get().contains(profession))
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

				// Multiply in the configured opacity value.
				currentAlpha *= MerchantMarkersConfig.INSTANCE.opacity.get();
			}

			float entityHeight = entity.getBbHeight() + 0.5F;
			int y = "deadmau5".equals(component.getString()) ? -28 : -18;
			y -= MerchantMarkersConfig.INSTANCE.verticalOffset.get();

			poseStack.pushPose();
			poseStack.translate(0.0D, (double)entityHeight, 0.0D);
			poseStack.mulPose(renderer.entityRenderDispatcher.cameraOrientation());
			poseStack.scale(-0.025F, -0.025F, 0.025F);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();

			boolean showArrow = MerchantMarkersConfig.INSTANCE.showArrow.get();

			if (MerchantMarkersConfig.INSTANCE.showThroughWalls.get())
			{
				RenderSystem.disableDepthTest();
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.3f * (float)currentAlpha);

				if (showArrow)
				{
					renderArrow(poseStack, 0, y);
				}

				renderMarker(getMarkerResource(mc, profession, level), poseStack, -8, showArrow ? y - 9 : y);
			}

			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float)currentAlpha);

			renderMarker(getMarkerResource(mc, profession, level), poseStack, -8, showArrow ? y - 9 : y);

			if (showArrow)
			{
				renderArrow(poseStack, 0, y);
			}

			poseStack.popPose();
		}
	}

	public static void clearResourceCache()
	{
		resourceCache.clear();
	}

	public static MarkerResource getMarkerResource(Minecraft mc, String professionName, int level)
	{
		String resourceKey = String.format("%s-%d", professionName, level);

		// Returned the cached value, if there is one.
		if (resourceCache.containsKey(resourceKey))
		{
			return resourceCache.get(resourceKey);
		}

		MarkerResource result = null;
		OverlayType overlayType = OverlayType.fromValue(MerchantMarkersConfig.INSTANCE.overlayIndex.get()).orElse(OverlayType.NONE);

		switch (MerchantMarkersConfig.MarkerType.fromText(MerchantMarkersConfig.INSTANCE.markerType.get()).get())
		{
			case ITEMS:
			{
				ResourceLocation associatedItemKey = MerchantMarkersConfig.INSTANCE.getAssociatedItem(professionName);
				if (associatedItemKey != null && Registry.ITEM.containsKey(associatedItemKey))
				{
					Item associatedItem = Registry.ITEM.getOptional(associatedItemKey).get();

					ItemRenderer itemRenderer = mc.getItemRenderer();
					BakedModel bakedModel = itemRenderer.getModel(new ItemStack(associatedItem), (Level)null, mc.player, 0);

					TextureAtlasSprite sprite = bakedModel.getParticleIcon();
					ResourceLocation spriteLocation = new ResourceLocation(sprite.getName().getNamespace(), String.format("textures/%s%s", sprite.getName().getPath(), ".png"));
					result = new MarkerResource(spriteLocation, overlayType, level);
				}
				break;
			}
			case JOBS:
			{
				// If the entity is a villager, find the (first) job block for their profession.
				VillagerProfession profession = Registry.VILLAGER_PROFESSION.get(new ResourceLocation(professionName.replace("__", ":")));
				if (profession != VillagerProfession.NONE)
				{
					Set<BlockState> jobBlockStates = profession.getJobPoiType().matchingStates;

					if (!jobBlockStates.isEmpty())
					{
						BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
						BakedModel bakedModel = blockRenderer.getBlockModel(jobBlockStates.iterator().next());

						TextureAtlasSprite sprite = bakedModel.getParticleIcon();
						ResourceLocation spriteLocation = new ResourceLocation(sprite.getName().getNamespace(), String.format("textures/%s%s", sprite.getName().getPath(), ".png"));
						result = new MarkerResource(spriteLocation, overlayType, level);
					}
				}
				break;
			}
			case CUSTOM:
			default:
			{
				// Check if the given resource exists, otherwise use the default icon.
				ResourceLocation iconResource = new ResourceLocation(Loader.MODID, String.format("textures/entity/villager/markers/%s.png", professionName));
				if (mc.getResourceManager().hasResource(iconResource))
				{
					result = new MarkerResource(iconResource, overlayType, level);
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
			result = new MarkerResource(DEFAULT_ICON, overlayType, level);
		}

		// Cache the result.
		resourceCache.put(resourceKey, result);
		return result;
	}

	private static void renderMarker(MarkerResource resource, PoseStack poseStack, int x, int y)
	{
		float scale = (float)(double)MerchantMarkersConfig.INSTANCE.iconScale.get();
		poseStack.pushPose();
		poseStack.scale(scale, scale, 1.0f);
		renderIcon(resource.texture(), poseStack, x, y);
		renderOverlay(resource, (dx, dy, width, height, sx, sy) -> {
			poseStack.translate(0, 0, -1);
			float imageSize = resource.overlay() == OverlayType.LEVEL ? 32.0f : 16.0f;
			renderIcon(resource.overlay() == OverlayType.LEVEL ? NUMBER_OVERLAY : ICON_OVERLAY, poseStack, x + dx, y + dy, width, height, sx / imageSize, (sx + width) / imageSize, sy / imageSize, (sy + height) / imageSize);
		});
		poseStack.popPose();
	}

	private static void renderArrow(PoseStack poseStack, int x, int y)
	{
		float scale = (float)(double)MerchantMarkersConfig.INSTANCE.iconScale.get();
		poseStack.pushPose();
		poseStack.scale(scale, scale, 1.0f);
		RenderSystem.setShaderTexture(0, MARKER_ARROW);
		GuiComponent.blit(poseStack, x - 8, y + 8, 0, 0, 16, 8, 16, 8);
		poseStack.popPose();
	}

	@FunctionalInterface
	public interface OverlayRendererMethod { void accept(int dx, int dy, int width, int height, int sx, int sy); }

	public static void renderOverlay(MarkerResource resource, OverlayRendererMethod method)
	{
		if (resource.overlay() == OverlayType.LEVEL)
		{
			renderOverlayLevel(resource, method);
		}
		else if (resource.overlay != OverlayType.NONE)
		{
			renderOverlayIcon(resource, method);
		}
	}

	private static void renderOverlayLevel(MarkerResource resource, OverlayRendererMethod method)
	{
		int processedDigits = resource.level();
		int xOffset = 8;

		// If the overlay is set to "profession level" and this marker has a level to show, add every digit needed.
		// Even though vanilla only supports a max level of 5, this should support any profession level.
		while (processedDigits > 0)
		{
			int currentDigit = processedDigits % 10;
			method.accept(xOffset, 8, 8, 8, (currentDigit % 4) * 8, (currentDigit / 4) * 8);
			processedDigits /= 10;
			xOffset -= 5;
		}
	}

	private static void renderOverlayIcon(MarkerResource resource, OverlayRendererMethod method)
	{
		method.accept(8, 8, 8, 8, (resource.overlay().value() % 2) * 8, (resource.overlay().value() / 2) * 8);
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
		bufferbuilder.vertex(matrix, (float)x,			(float)(y + h),		0).uv(u0, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)(y + h),		0).uv(u1, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)y,			0).uv(u1, v0).endVertex();
		bufferbuilder.vertex(matrix, (float)x,			(float)y,			0).uv(u0, v0).endVertex();
		bufferbuilder.end();
		BufferUploader.end(bufferbuilder);
	}
}
