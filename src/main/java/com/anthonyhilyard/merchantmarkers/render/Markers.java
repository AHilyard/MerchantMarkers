package com.anthonyhilyard.merchantmarkers.render;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig.OverlayType;
import com.anthonyhilyard.merchantmarkers.util.NullInputStream;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.math.vector.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resources.IResourceManager;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class Markers
{
	public static class MarkerResource
	{
		public final ResourceLocation texture;
		public final OverlayType overlay;
		public final int level;
		public MarkerResource(ResourceLocation texture, OverlayType overlay, int level)
		{
			this.texture = texture;
			this.overlay = overlay;
			this.level = level;
		}

		@Override
		public int hashCode() { return Objects.hash(texture, overlay, level); }

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			else if (!(obj instanceof MarkerResource))
			{
				return false;
			}
			else
			{
				MarkerResource other = (MarkerResource) obj;
				return Objects.equals(texture, other.texture) &&
					   Objects.equals(overlay, other.overlay) &&
					   Objects.equals(level, other.level);
			}
		}
	}

	public static final ResourceLocation MARKER_ARROW = new ResourceLocation(Loader.MODID, "textures/entity/villager/arrow.png");
	public static final ResourceLocation ICON_OVERLAY = new ResourceLocation(Loader.MODID, "textures/entity/villager/overlay.png");
	public static final ResourceLocation NUMBER_OVERLAY = new ResourceLocation(Loader.MODID, "textures/entity/villager/numbers.png");
	public static final ResourceLocation DEFAULT_ICON = new ResourceLocation(Loader.MODID, "textures/entity/villager/default.png");
	public static final ResourceLocation EMPTY_MARKER = new ResourceLocation(Loader.MODID, "textures/entity/villager/empty.png");

	private static Supplier<InputStream> emptyMarkerResource = null;
	
	private static Map<String, MarkerResource> resourceCache = new HashMap<>();

	public static InputStream getEmptyInputStream()
	{
		if (emptyMarkerResource == null)
		{
			emptyMarkerResource = () -> {
				final Minecraft mc = Minecraft.getInstance();
				final IResourceManager manager = mc.getResourceManager();
				try
				{
					return manager.getResource(Markers.EMPTY_MARKER).getInputStream();
				}
				catch (Exception e)
				{
					// Don't do anything, maybe the resource pack just isn't ready yet.
					return NullInputStream.stream();
				}
			};
		}
		return emptyMarkerResource.get();
	}

	public static String getProfessionName(Entity entity)
	{
		String iconName = "default";
		if (entity instanceof VillagerEntity)
		{
			// If the profession name contains any colons, replace them with double underscores.
			iconName = ((VillagerEntity)entity).getVillagerData().getProfession().toString().replace(":","__");
		}
		else if (entity instanceof WanderingTraderEntity)
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
		if (MerchantMarkersConfig.INSTANCE.showLevels() && entity instanceof VillagerEntity)
		{
			level = ((VillagerEntity)entity).getVillagerData().getLevel();
		}
		return level;
	}

	public static void renderMarker(EntityRenderer<?> renderer, Entity entity, ITextComponent component, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight)
	{
		if (entity instanceof AbstractVillagerEntity)
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
			float currentAlpha = 1.0f;
			
			// We won't do any calculations if fadePercent is 100, since that would make a division by zero.
			if (fadePercent < 100.0)
			{
				// Calculate the distance at which fading begins.
				double startFade = ((1.0 - (fadePercent / 100.0)) * maxDistance);

				// Calculate the current alpha value for this marker.
				currentAlpha = (float)MathHelper.clamp(1.0 - ((Math.sqrt(squareDistance) - startFade) / (maxDistance - startFade)), 0.0, 1.0);
			}

			float entityHeight = entity.getBbHeight() + 0.5F;
			int y = "deadmau5".equals(component.getString()) ? -28 : -18;
			y -= MerchantMarkersConfig.INSTANCE.verticalOffset.get();

			matrixStack.pushPose();
			matrixStack.translate(0.0D, (double)entityHeight, 0.0D);
			matrixStack.mulPose(renderer.entityRenderDispatcher.cameraOrientation());
			matrixStack.scale(-0.025F, -0.025F, 0.025F);

			boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
			boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();

			boolean showArrow = MerchantMarkersConfig.INSTANCE.showArrow.get();

			if (MerchantMarkersConfig.INSTANCE.showThroughWalls.get())
			{
				RenderSystem.disableDepthTest();

				renderMarker(getMarkerResource(mc, profession, level), matrixStack, -8, showArrow ? y - 9 : y, 0.3f * currentAlpha);

				if (showArrow)
				{
					renderArrow(matrixStack, 0, y, 0.3f * currentAlpha);
				}
			}

			RenderSystem.enableDepthTest();

			renderMarker(getMarkerResource(mc, profession, level), matrixStack, -8, showArrow ? y - 9 : y, currentAlpha);

			if (showArrow)
			{
				renderArrow(matrixStack, 0, y, currentAlpha);
			}

			// Revert depth test to original state.
			if (depthTestEnabled)
			{
				RenderSystem.enableDepthTest();
			}
			else
			{
				RenderSystem.disableDepthTest();
			}

			if (blendEnabled)
			{
				RenderSystem.enableBlend();
			}
			else
			{
				RenderSystem.disableBlend();
			}


			matrixStack.popPose();
		}
	}

	public static void clearResourceCache()
	{
		resourceCache.clear();
	}

	@SuppressWarnings("deprecation")
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
				if (associatedItemKey != null)
				{
					Item associatedItem = ForgeRegistries.ITEMS.getValue(associatedItemKey);

					ItemRenderer itemRenderer = mc.getItemRenderer();
					IBakedModel bakedModel = itemRenderer.getModel(new ItemStack(associatedItem), (World)null, mc.player);

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
					Set<BlockState> jobBlockStates = profession.getJobPoiType().getBlockStates();

					if (!jobBlockStates.isEmpty())
					{
						BlockRendererDispatcher blockRenderer = mc.getBlockRenderer();
						IBakedModel bakedModel = blockRenderer.getBlockModel(jobBlockStates.iterator().next());

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

	private static void renderMarker(MarkerResource resource, MatrixStack matrixStack, int x, int y, float alpha)
	{
		float scale = (float)(double)MerchantMarkersConfig.INSTANCE.iconScale.get();
		matrixStack.pushPose();
		matrixStack.scale(scale, scale, 1.0f);
		renderIcon(resource.texture, matrixStack, x, y, alpha);
		renderOverlay(resource, (dx, dy, width, height, sx, sy) -> {
			matrixStack.translate(0, 0, -1);
			float imageSize = resource.overlay == OverlayType.LEVEL ? 32.0f : 16.0f;
			renderIcon(resource.overlay == OverlayType.LEVEL ? NUMBER_OVERLAY : ICON_OVERLAY, matrixStack, x + dx, y + dy, width, height, sx / imageSize, (sx + width) / imageSize, sy / imageSize, (sy + height) / imageSize, alpha);
		});
		matrixStack.popPose();
	}

	private static void renderArrow(MatrixStack matrixStack, int x, int y, float alpha)
	{
		float scale = (float)(double)MerchantMarkersConfig.INSTANCE.iconScale.get();
		matrixStack.pushPose();
		matrixStack.scale(scale, scale, 1.0f);
		renderIcon(MARKER_ARROW, matrixStack, x - 8, y + 8, 16, 8, 0, 1, 0, 1, alpha);
		matrixStack.popPose();
	}

	@FunctionalInterface
	public interface OverlayRendererMethod { void accept(int dx, int dy, int width, int height, int sx, int sy); }

	public static void renderOverlay(MarkerResource resource, OverlayRendererMethod method)
	{
		if (resource.overlay == OverlayType.LEVEL)
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
		int processedDigits = resource.level;
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
		method.accept(8, 8, 8, 8, (resource.overlay.value() % 2) * 8, (resource.overlay.value() / 2) * 8);
	}

	private static void renderIcon(ResourceLocation icon, MatrixStack matrixStack, int x, int y, float alpha)
	{
		renderIcon(icon, matrixStack, x, y, 16, 16, 0, 1, 0, 1, alpha);
	}

	private static void renderIcon(ResourceLocation icon, MatrixStack matrixStack, int x, int y, int w, int h, float u0, float u1, float v0, float v1, float alpha)
	{
		Minecraft mc = Minecraft.getInstance();
		Matrix4f matrix = matrixStack.last().pose();

		mc.getTextureManager().bind(icon);

		BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
		bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.vertex(matrix, (float)x,			(float)(y + h),	0).color(1.0f, 1.0f, 1.0f, alpha).uv(u0, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)(y + h),	0).color(1.0f, 1.0f, 1.0f, alpha).uv(u1, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)y,		0).color(1.0f, 1.0f, 1.0f, alpha).uv(u1, v0).endVertex();
		bufferbuilder.vertex(matrix, (float)x,			(float)y,		0).color(1.0f, 1.0f, 1.0f, alpha).uv(u0, v0).endVertex();
		bufferbuilder.end();

		WorldVertexBufferUploader.end(bufferbuilder);
	}
}
