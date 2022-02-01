package com.anthonyhilyard.merchantmarkers;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import com.anthonyhilyard.iceberg.util.DynamicResourcePack;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig.OverlayType;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.anthonyhilyard.merchantmarkers.render.Markers.MarkerResource;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;

import xaero.common.minimap.render.radar.EntityIconDefinitions;
import xaero.minimap.XaeroMinimap;

public class XaeroHandler implements ResourceManagerReloadListener
{
	private static XaeroHandler INSTANCE = new XaeroHandler();
	private static Map<MarkerResource, byte[]> iconCache = new HashMap<>();
	private static BufferedImage iconOverlayImage = null;
	private static BufferedImage numberOverlayImage = null;
	private static DynamicResourcePack dynamicPack = new DynamicResourcePack("dynamicicons");

	public static void buildVariantIdString(final StringBuilder stringBuilder, final EntityRenderer<?> entityRenderer, final Entity entity)
	{
		// If the profession blacklist contains this profession, run the default functionality.
		String profession = Markers.getProfessionName(entity);
		int professionLevel = Markers.getProfessionLevel(entity);
		if (MerchantMarkersConfig.INSTANCE.professionBlacklist.get().contains(profession))
		{
			EntityIconDefinitions.buildVariantIdString(stringBuilder, entityRenderer, entity);
		}
		// Otherwise, we'll fill in a standin identifier for a dynamically-populated icon (see below).
		else
		{
			stringBuilder.append(profession).append("-").append(professionLevel);
		}
	}

	public static void clearIconCache()
	{
		// Clear our local cache.
		iconCache.clear();

		// Reset our dynamic resources.
		dynamicPack.clear();
		setupDynamicIcons();

		// Clear the minimap icon resources cache.
		if (XaeroMinimap.instance != null && XaeroMinimap.instance.getInterfaces() != null)
		{
			XaeroMinimap.instance.getInterfaces().getMinimapInterface().getMinimapFBORenderer().resetEntityIconsResources();
		}
	}

	private static InputStream getResizedIcon(Supplier<MarkerResource> resourceSupplier)
	{
		MarkerResource resource = resourceSupplier.get();
		if (resource == null)
		{
			return InputStream.nullInputStream();
		}

		if (iconCache.containsKey(resource))
		{
			return new ByteArrayInputStream(iconCache.get(resource));
		}

		final int innerSize = (int)(32 * MerchantMarkersConfig.INSTANCE.minimapIconScale.get());
		final int outerSize = 64;

		ResourceManager manager = Minecraft.getInstance().getResourceManager();

		BufferedImage newImage = new BufferedImage(outerSize, outerSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = newImage.createGraphics();
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		// Maybe it's just not loaded yet?  Bail for now.
		if (!manager.hasResource(resource.texture()) && Minecraft.getInstance().getTextureManager().getTexture(resource.texture(), null) == null)
		{
			return InputStream.nullInputStream();
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
			try (os)
			{
				ImageIO.write(newImage, "png", os);
				iconCache.put(resource, os.toByteArray());
				return new ByteArrayInputStream(iconCache.get(resource));
			}
		}
		catch (Exception e)
		{ 
			Loader.LOGGER.error(e.toString());
		}

		iconCache.put(resource, new byte[0]);
		return InputStream.nullInputStream();
	}

	@SuppressWarnings("resource")
	public static void setupDynamicIcons()
	{
		final Minecraft minecraft = Minecraft.getInstance();
		ResourceManager manager = minecraft.getResourceManager();

		if (manager instanceof SimpleReloadableResourceManager)
		{
			SimpleReloadableResourceManager reloadableManager = (SimpleReloadableResourceManager)manager;
			Supplier<Collection<ResourceLocation>> delayedResources = () -> reloadableManager.listResources("textures/entity/villager/markers", s -> s.endsWith(".png"));

			if (!reloadableManager.listeners.contains(INSTANCE))
			{
				reloadableManager.listeners.add(0, INSTANCE);
			}

			// If we're showing icons on the minimap, setup proxies for the villager icon definitions and icons themselves.
			if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get())
			{
				final int minLevel;
				final int maxLevel;

				// If level display is turned off, we only care about level 0, which means "don't show level".
				if (!MerchantMarkersConfig.INSTANCE.showLevels())
				{
					minLevel = maxLevel = 0;
				}
				else
				{
					minLevel = 0;
					maxLevel = VillagerData.MAX_VILLAGER_LEVEL + 10;
				}

				dynamicPack.registerResource(PackType.CLIENT_RESOURCES, new ResourceLocation("xaerominimap", "entity/icon/definition/minecraft/villager.json"), () -> {

					// Dynamically build the .json file to include all current villager markers available, with dynamic proxies for each.
					JsonObject variants = new JsonObject();
					for (ResourceLocation marker : delayedResources.get())
					{
						for (int i = minLevel; i <= maxLevel; i++)
						{
							String[] components = marker.getPath().split("/");
							String iconName = components[components.length - 1].replace(".png", "");
							variants.addProperty(iconName + "-" + String.valueOf(i), "sprite:" + marker.getPath().replace(".png",  "-" + String.valueOf(i) + ".png"));
						}
					}

					JsonObject result = new JsonObject();
					result.addProperty("variantIdBuilderMethod", "com.anthonyhilyard.merchantmarkers.XaeroHandler.buildVariantIdString");
					result.add("variants", variants);

					return new ByteArrayInputStream(result.toString().getBytes());
				});

				for (ResourceLocation marker : delayedResources.get())
				{
					String[] components = marker.getPath().split("/");
					String iconName = components[components.length - 1].replace(".png", "");

					for (int i = minLevel; i <= maxLevel; i++)
					{
						final int level = i;
						ResourceLocation markerLocation = new ResourceLocation("xaerominimap", "entity/icon/sprite/" + marker.getPath().replace(".png", "-" + String.valueOf(i) + ".png"));

						// If this location is already registered in Minecraft's texture manager, release it first.
						if (minecraft.getTextureManager().getTexture(markerLocation, null) != null)
						{
							minecraft.execute(() -> minecraft.getTextureManager().release(markerLocation));
						}

						// Register a proxy resource to display our chosen icon.
						dynamicPack.registerResource(PackType.CLIENT_RESOURCES, markerLocation, () -> {
							try
							{
								InputStream proxyStream = getResizedIcon(() -> Markers.getMarkerResource(minecraft, iconName, level));
								if (proxyStream.available() == 0)
								{
									return reloadableManager.getResource(Markers.getMarkerResource(minecraft, iconName, level).texture()).getInputStream();
								}
								else
								{
									return proxyStream;
								}
							}
							catch (Exception e)
							{
								return InputStream.nullInputStream();
							}
						});
					}
				}
			}
			// If we're not showing icons on the minimap, just setup a default icons definition file.
			else
			{
				dynamicPack.registerResource(PackType.CLIENT_RESOURCES, new ResourceLocation("xaerominimap", "entity/icon/definition/minecraft/villager.json"), () -> {

					// Dynamically build the .json file to include all current villager markers available, with dynamic proxies for each.
					JsonObject variants = new JsonObject();
					variants.addProperty("default", "model");

					JsonObject result = new JsonObject();
					result.addProperty("variantIdBuilderMethod", "xaero.common.minimap.render.radar.EntityIconDefinitions.buildVariantIdString");
					result.add("variants", variants);

					return new ByteArrayInputStream(result.toString().getBytes());
				});
			}

			// Add the resource pack if it hasn't been added already.
			if (!reloadableManager.listPacks().anyMatch(pack -> pack.equals(dynamicPack)))
			{
				reloadableManager.add(dynamicPack);
			}
		}
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager)
	{
		if (resourceManager instanceof SimpleReloadableResourceManager)
		{
			Markers.clearResourceCache();
			clearIconCache();
		}
	}
}
