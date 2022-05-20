package com.anthonyhilyard.merchantmarkers.compat;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import com.anthonyhilyard.iceberg.util.DynamicResourcePack;
import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig.OverlayType;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.anthonyhilyard.merchantmarkers.render.Markers.MarkerResource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

public class FTBChunksHandler implements ISelectiveResourceReloadListener
{
	private static FTBChunksHandler INSTANCE = new FTBChunksHandler();
	private static DynamicResourcePack dynamicPack = new DynamicResourcePack("dynamicicons");
	private static Entity currentEntity = null;
	private static Map<MarkerResource, byte[]> iconCache = new HashMap<>();
	private static BufferedImage iconOverlayImage = null;
	private static BufferedImage numberOverlayImage = null;

	public static ResourceLocation villagerTexture = new ResourceLocation("ftbchunks", "textures/faces/minecraft/villager.png");
	private static Supplier<InputStream> defaultVillagerResource = null;

	public static void setCurrentEntity(Entity entity)
	{
		currentEntity = entity;

		if (entity instanceof VillagerEntity)
		{
			Minecraft mc = Minecraft.getInstance();

			// If this location is already registered in Minecraft's texture manager, release it first.
			if (mc.getTextureManager().getTexture(villagerTexture) != null)
			{
				mc.execute(() -> mc.getTextureManager().release(villagerTexture));
			}
		}
	}

	public static void clearIconCache()
	{
		// Clear our local cache.
		iconCache.clear();

		// Reset our dynamic resources.
		dynamicPack.clear();
		setupDynamicIcons();
	}

	private static InputStream getResizedIcon(Supplier<MarkerResource> resourceSupplier)
	{
		MarkerResource resource = resourceSupplier.get();
		if (resource == null)
		{
			return Markers.getEmptyInputStream();
		}

		if (iconCache.containsKey(resource))
		{
			return new ByteArrayInputStream(iconCache.get(resource));
		}

		final int innerSize = (int)(32 * MerchantMarkersConfig.INSTANCE.minimapIconScale.get());
		final int outerSize = innerSize;

		IResourceManager manager = Minecraft.getInstance().getResourceManager();

		BufferedImage newImage = new BufferedImage(outerSize, outerSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = newImage.createGraphics();
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		// Maybe it's just not loaded yet?  Bail for now.
		if (!manager.hasResource(resource.texture) && Minecraft.getInstance().getTextureManager().getTexture(resource.texture) == null)
		{
			return Markers.getEmptyInputStream();
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

			BufferedImage originalImage = ImageIO.read(manager.getResource(resource.texture).getInputStream());
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
				BufferedImage overlayImage = resource.overlay == OverlayType.LEVEL ? numberOverlayImage : iconOverlayImage;
				final float scale = (innerSize / (float)originalImage.getWidth());
				graphics.drawImage(overlayImage,
								  (int)(left + dx * scale), (int)(top - dy * scale),
								  (int)(left + (dx + width) * scale), (int)(top - (dy + height) * scale),
								  sx, sy, sx + width, sy + height, null);
			});
			graphics.dispose();

			// Convert the image to an input stream and return it.
			try
			{
				ImageIO.write(newImage, "png", os);
				iconCache.put(resource, os.toByteArray());
				return new ByteArrayInputStream(iconCache.get(resource));
			}
			finally
			{
				os.close();
			}
		}
		catch (Exception e)
		{ 
			Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
		}

		iconCache.put(resource, new byte[0]);
		return Markers.getEmptyInputStream();
	}

	@SuppressWarnings("resource")
	public static void setupDynamicIcons()
	{
		Minecraft mc = Minecraft.getInstance();
		IResourceManager manager = mc.getResourceManager();

		if (manager instanceof SimpleReloadableResourceManager)
		{
			SimpleReloadableResourceManager reloadableManager = (SimpleReloadableResourceManager)manager;

			if (!reloadableManager.listeners.contains(INSTANCE))
			{
				reloadableManager.listeners.add(0, INSTANCE);
			}

			// If we haven't grabbed the default villager texture yet, do so now.
			if (defaultVillagerResource == null)
			{
				try
				{
					for (IResource resource : reloadableManager.getResources(villagerTexture))
					{
						// Return the first non-dynamic villager texture.
						if (!resource.getSourceName().contentEquals("dynamicicons"))
						{
							final byte[] defaultVillagerBytes = IOUtils.toByteArray(resource.getInputStream());
							defaultVillagerResource = () -> { return new ByteArrayInputStream(defaultVillagerBytes); };
							break;
						}
					}
				}
				catch (Exception e)
				{
					// Don't do anything, maybe the resource pack just isn't ready yet.
				}
			}

			dynamicPack.registerResource(ResourcePackType.CLIENT_RESOURCES, villagerTexture, () -> {

				if (currentEntity == null || (currentEntity instanceof VillagerEntity && ((VillagerEntity)currentEntity).isBaby()))
				{
					return Markers.getEmptyInputStream();
				}

				try
				{
					String profession = Markers.getProfessionName(currentEntity);
					int level = Markers.getProfessionLevel(currentEntity);

					// Return the default texture for blacklisted professions.
					if (MerchantMarkersConfig.INSTANCE.professionBlacklist.get().contains(profession))
					{
						return defaultVillagerResource == null ? Markers.getEmptyInputStream() : defaultVillagerResource.get();
					}

					InputStream proxyStream = getResizedIcon(() -> Markers.getMarkerResource(mc, profession, level));

					// Stupid workaround, I know.  For some reason the proxy stream is sometimes not ready when it is returned,
					// must be some sort of threaded timing issue?  In any case, this works.
					Thread.sleep(1);

					if (proxyStream.available() == 0)
					{
						return reloadableManager.getResource(Markers.getMarkerResource(mc, profession, level).texture).getInputStream();
					}
					else
					{
						return proxyStream;
					}
				}
				catch (Exception e)
				{
					return Markers.getEmptyInputStream();
				}
			});

			// Add the resource pack if it hasn't been added already.
			if (!reloadableManager.listPacks().anyMatch(pack -> pack.equals(dynamicPack)))
			{
				reloadableManager.add(dynamicPack);
			}
		}
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
	{
		if (resourcePredicate.test(VanillaResourceType.TEXTURES))
		{
			Markers.clearResourceCache();
			clearIconCache();
		}
	}
}
