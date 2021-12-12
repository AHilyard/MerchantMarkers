package com.anthonyhilyard.merchantmarkers.mixin;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig.OverlayType;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.anthonyhilyard.merchantmarkers.render.Markers.MarkerResource;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import journeymap.client.render.draw.DrawEntityStep;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.texture.TextureImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;

@Mixin(value = DrawEntityStep.class, remap = false)
public class JourneyMapDrawEntityStepMixin
{
	@Shadow
	Minecraft minecraft;

	@Shadow
	WeakReference<LivingEntity> entityLivingRef;

	private static BufferedImage iconOverlayImage = null;
	private static BufferedImage numberOverlayImage = null;

	private static Map<MarkerResource, TextureImpl> textureCache = new HashMap<>();

	@Redirect(method = "drawCreature", at = @At(value = "INVOKE", target = "Ljourneymap/client/render/draw/DrawUtil;drawEntity(Lcom/mojang/blaze3d/vertex/PoseStack;DDDLjourneymap/client/render/texture/TextureImpl;FFD)V", remap = false))
	private void drawEntity(PoseStack poseStack, double x, double y, double heading, TextureImpl textureIn, float alpha, float scale, double rotation)
	{
		LivingEntity entityLiving = entityLivingRef.get();
		TextureImpl texture = textureIn;

		// If this entity is marker-able, update the texture before drawing.
		if (entityLiving instanceof AbstractVillager && !((AbstractVillager)entityLiving).isBaby())
		{
			String profession = Markers.getProfessionName(entityLiving);
			int level = Markers.getProfessionLevel(entityLiving);
			MarkerResource markerResource = Markers.getMarkerResource(minecraft, profession, level);
	
			if (!textureCache.containsKey(markerResource))
			{
				textureCache.put(markerResource, new TextureImpl(getMarkerImage(markerResource), true));
			}
			texture = textureCache.get(markerResource);
		}

		DrawUtil.drawEntity(poseStack, x, y, heading, texture, alpha, scale, rotation);
	}

	private BufferedImage getMarkerImage(MarkerResource resource)
	{
		final int innerSize = (int)(32 * MerchantMarkersConfig.INSTANCE.minimapIconScale.get());
		final int outerSize = 64;

		ResourceManager manager = Minecraft.getInstance().getResourceManager();

		BufferedImage newImage = new BufferedImage(outerSize, outerSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = newImage.createGraphics();

		if (!manager.hasResource(resource.texture()) && Minecraft.getInstance().getTextureManager().getTexture(resource.texture(), null) == null)
		{
			return null;
		}

		try
		{
			// Lazy-load the overlay images now if needed.
			if (iconOverlayImage == null)
			{
				iconOverlayImage = ImageIO.read(manager.getResource(Markers.ICON_OVERLAY).getInputStream());
			}
			if (numberOverlayImage == null)
			{
				numberOverlayImage = ImageIO.read(manager.getResource(Markers.NUMBER_OVERLAY).getInputStream());
			}

			BufferedImage originalImage = ImageIO.read(manager.getResource(resource.texture()).getInputStream());
			final int left = (outerSize - innerSize) / 2;
			final int right = (outerSize + innerSize) / 2;
			final int top = (outerSize + innerSize) / 2;
			final int bottom = (outerSize - innerSize) / 2;

			// Flip the image vertically.
			AffineTransform at = new AffineTransform();
			at.concatenate(AffineTransform.getScaleInstance(1, -1));
			at.concatenate(AffineTransform.getTranslateInstance(0, -newImage.getHeight()));
			graphics.transform(at);

			// Draw the icon centered in the new image.
			graphics.drawImage(originalImage, left, top, right, bottom,
							0, 0, originalImage.getWidth(), originalImage.getHeight(), null);

			// Also draw the overlay graphic.
			Markers.renderOverlay(resource, (dx, dy, width, height, sx, sy) -> {
				BufferedImage overlayImage = resource.overlay() == OverlayType.LEVEL ? numberOverlayImage : iconOverlayImage;
				final float scale = (innerSize / (float)originalImage.getWidth());
				graphics.drawImage(overlayImage,
								(int)(left + dx * scale), (int)(top - dy * scale),
								(int)(left + (dx + width) * scale), (int)(top - (dy + height) * scale),
								sx, sy, sx + width, sy + height, null);
			});
			graphics.dispose();
		}
		catch (Exception e)
		{
			return null;
		}

		return newImage;
	}
}
