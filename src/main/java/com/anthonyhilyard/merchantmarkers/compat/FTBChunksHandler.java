package com.anthonyhilyard.merchantmarkers.compat;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ConcurrentModificationException;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.resources.ResourceLocation;

public class FTBChunksHandler implements ResourceManagerReloadListener
{
	private static FTBChunksHandler INSTANCE = new FTBChunksHandler();
	private static DynamicResourcePack dynamicPack = new DynamicResourcePack("dynamicicons");
	private static Entity currentEntity = null;
	private static Map<MarkerResource, byte[]> iconCache = new HashMap<>();
	private static BufferedImage iconOverlayImage = null;
	private static BufferedImage numberOverlayImage = null;

	public static final ResourceLocation villagerTexture = new ResourceLocation("ftbchunks", "textures/faces/minecraft/villager.png");
	private static Supplier<InputStream> defaultVillagerResource  = null;

	public static void setCurrentEntity(Entity entity)
	{
		currentEntity = entity;

		if (Markers.shouldShowMarker(entity))
		{
			final Minecraft minecraft = Minecraft.getInstance();

			// If this location is already registered in Minecraft's texture manager, release it first.
			if (minecraft.getTextureManager().getTexture(villagerTexture, null) != null)
			{
				minecraft.executeBlocking(() -> minecraft.getTextureManager().release(villagerTexture));
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

		final int innerSize = (int)(32 * MerchantMarkersConfig.getInstance().minimapIconScale.get());
		final int outerSize = innerSize;

		ResourceManager manager = Minecraft.getInstance().getResourceManager();

		BufferedImage newImage = new BufferedImage(outerSize, outerSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = newImage.createGraphics();
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		// Maybe it's just not loaded yet?  Bail for now.
		if (manager.getResource(resource.texture()).isEmpty() && Minecraft.getInstance().getTextureManager().getTexture(resource.texture()) == null)
		{
			return Markers.getEmptyInputStream();
		}

		try
		{
			// Lazy-load the overlay images now if needed.
			if (iconOverlayImage == null)
			{
				iconOverlayImage = ImageIO.read(manager.getResource(Markers.ICON_OVERLAY).get().open());
			}
			if (numberOverlayImage == null)
			{
				numberOverlayImage = ImageIO.read(manager.getResource(Markers.NUMBER_OVERLAY).get().open());
			}

			BufferedImage originalImage = ImageIO.read(manager.getResource(resource.texture()).get().open());
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
		ResourceManager manager = mc.getResourceManager();

		if (manager instanceof ReloadableResourceManager)
		{
			ReloadableResourceManager reloadableManager = (ReloadableResourceManager)manager;

			if (!reloadableManager.listeners.contains(INSTANCE))
			{
				reloadableManager.listeners.add(0, INSTANCE);
			}

			// If we haven't grabbed the default villager texture yet, do so now.
			if (defaultVillagerResource == null)
			{
				try
				{
					for (Resource resource : reloadableManager.getResourceStack(villagerTexture))
					{
						// Return the first non-dynamic villager texture.
						if (!resource.sourcePackId().contentEquals("dynamicicons"))
						{
							final byte[] defaultVillagerBytes = IOUtils.toByteArray(resource.open());
							defaultVillagerResource = () -> {
								return new ByteArrayInputStream(defaultVillagerBytes); 
							};
							break;
						}
					}
				}
				catch (Exception e)
				{
					// Don't do anything, maybe the resource pack just isn't ready yet.
				}
			}

			dynamicPack.registerResource(PackType.CLIENT_RESOURCES, villagerTexture, () -> {

				if (currentEntity == null || (!Markers.shouldShowMarker(currentEntity)))
				{
					return Markers.getEmptyInputStream();
				}

				try
				{
					String profession = Markers.getProfessionName(currentEntity);
					int level = Markers.getProfessionLevel(currentEntity);

					// Return the default texture for blacklisted professions.
					if (MerchantMarkersConfig.getInstance().professionBlacklist.get().contains(profession))
					{
						return defaultVillagerResource == null ? Markers.getEmptyInputStream() : defaultVillagerResource.get();
					}

					InputStream proxyStream = getResizedIcon(() -> Markers.getMarkerResource(mc, profession, level));

					// Stupid workaround, I know.  For some reason the proxy stream is sometimes not ready when it is returned,
					// must be some sort of threaded timing issue?  In any case, this works.
					Thread.sleep(5);

					if (proxyStream.available() == 0)
					{
						return reloadableManager.getResource(Markers.getMarkerResource(mc, profession, level).texture()).get().open();
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
				if (reloadableManager.resources instanceof MultiPackResourceManager resourceManager)
				{
					try
					{
						reloadableManager.resources.close();
					}
					catch (ConcurrentModificationException e) { /* Oops. */ }

					reloadableManager.resources = new MultiPackResourceManager(reloadableManager.type, Stream.concat(resourceManager.listPacks(), Stream.of(dynamicPack)).toList());
				}
			}
		}
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager)
	{
		Markers.clearResourceCache();
		clearIconCache();
	}
}