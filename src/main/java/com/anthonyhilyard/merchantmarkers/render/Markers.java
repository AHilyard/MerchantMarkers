package com.anthonyhilyard.merchantmarkers.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.math.vector.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
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
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class Markers
{
	public static class MarkerResource
	{
		public final ResourceLocation texture;
		public final boolean overlay;
		public MarkerResource(ResourceLocation texture, boolean overlay)
		{
			this.texture = texture;
			this.overlay = overlay;
		}

		@Override
		public int hashCode() { return Objects.hash(texture, overlay); }

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
					   Objects.equals(overlay, other.overlay);
			}
		}
	}

	public static final ResourceLocation MARKER_ARROW = new ResourceLocation(Loader.MODID, "textures/entity/villager/arrow.png");
	public static final ResourceLocation ICON_OVERLAY = new ResourceLocation(Loader.MODID, "textures/entity/villager/overlay.png");
	public static final ResourceLocation DEFAULT_ICON = new ResourceLocation(Loader.MODID, "textures/entity/villager/default.png");

	private static Map<String, MarkerResource> resourceCache = new HashMap<>();

	public static String getIconName(Entity entity)
	{
		String iconName = "default";
		if (entity instanceof VillagerEntity)
		{
			// If the profession name contains and colons, replace them with underscores.
			iconName = ((VillagerEntity)entity).getVillagerData().getProfession().toString().replace(":","_");
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

	@SuppressWarnings("deprecation")
	public static void renderMarker(EntityRenderer<?> renderer, Entity entity, ITextComponent component, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight)
	{
		if (entity instanceof AbstractVillagerEntity)
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
				currentAlpha = MathHelper.clamp(1.0 - ((Math.sqrt(squareDistance) - startFade) / (maxDistance - startFade)), 0.0, 1.0);
			}

			float f = entity.getBbHeight() + 0.5F;
			int i = "deadmau5".equals(component.getString()) ? -28 : -18;
			matrixStack.pushPose();
			matrixStack.translate(0.0D, (double)f, 0.0D);
			matrixStack.mulPose(renderer.entityRenderDispatcher.cameraOrientation());
			matrixStack.scale(-0.025F, -0.025F, 0.025F);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableDepthTest();
			RenderSystem.color4f(1.0f, 1.0f, 1.0f, 0.3f * (float)currentAlpha);

			boolean showArrow = MerchantMarkersConfig.INSTANCE.showArrow.get();

			if (showArrow)
			{
				mc.getTextureManager().bind(MARKER_ARROW);
				AbstractGui.blit(matrixStack, -8, i + 8, 0, 0, 16, 8, 16, 8);
			}

			renderMarker(getMarkerResource(mc, iconName), matrixStack, -8, showArrow ? i - 9 : i);

			RenderSystem.enableDepthTest();
			RenderSystem.color4f(1.0f, 1.0f, 1.0f, (float)currentAlpha);

			renderMarker(getMarkerResource(mc, iconName), matrixStack, -8, showArrow ? i - 9 : i);

			if (showArrow)
			{
				mc.getTextureManager().bind(MARKER_ARROW);
				AbstractGui.blit(matrixStack, -8, i + 8, 0, 0, 16, 8, 16, 8);
			}

			matrixStack.popPose();
		}
	}

	public static void clearResourceCache()
	{
		resourceCache.clear();
	}

	@SuppressWarnings("deprecation")
	public static MarkerResource getMarkerResource(Minecraft mc, String iconName)
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
					IBakedModel bakedModel = itemRenderer.getModel(new ItemStack(associatedItem), (World)null, mc.player);

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
						BlockRendererDispatcher blockRenderer = mc.getBlockRenderer();
						IBakedModel bakedModel = blockRenderer.getBlockModel(jobBlockStates.iterator().next());

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

	private static void renderMarker(MarkerResource resource, MatrixStack matrixStack, int x, int y)
	{
		renderIcon(resource.texture, matrixStack, x, y);
		if (resource.overlay)
		{
			int overlayIndex = MerchantMarkersConfig.INSTANCE.overlayIndex.get();
			if (overlayIndex == -1)
			{
				return;
			}

			matrixStack.pushPose();
			matrixStack.translate(0, 0, -1);
			renderIcon(ICON_OVERLAY, matrixStack, x + 8, y + 8, 8, 8, (overlayIndex % 2) * 0.5f, (overlayIndex % 2) * 0.5f + 0.5f, (overlayIndex / 2) * 0.5f, (overlayIndex / 2) * 0.5f + 0.5f);
			matrixStack.popPose();
		}
	}

	private static void renderIcon(ResourceLocation icon, MatrixStack matrixStack, int x, int y)
	{
		renderIcon(icon, matrixStack, x, y, 16, 16, 0, 1, 0, 1);
	}

	private static void renderIcon(ResourceLocation icon, MatrixStack matrixStack, int x, int y, int w, int h, float u0, float u1, float v0, float v1)
	{
		Minecraft mc = Minecraft.getInstance();
		Matrix4f matrix = matrixStack.last().pose();

		mc.getTextureManager().bind(icon);

		BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
		bufferbuilder.vertex(matrix, (float)x,			(float)(y + h),	0).uv(u0, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)(y + h),	0).uv(u1, v1).endVertex();
		bufferbuilder.vertex(matrix, (float)(x + w),	(float)y,			0).uv(u1, v0).endVertex();
		bufferbuilder.vertex(matrix, (float)x,			(float)y,			0).uv(u0, v0).endVertex();
		bufferbuilder.end();
		WorldVertexBufferUploader.end(bufferbuilder);
	}
}
