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
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.anthonyhilyard.merchantmarkers.render.Markers.MarkerResource;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
	private static BufferedImage overlayImage = null;
	private static DynamicResourcePack dynamicPack = new DynamicResourcePack("dynamicicons");

	public static void buildVariantIdString(final StringBuilder stringBuilder, final EntityRenderer<?> entityRenderer, final Entity entity)
	{
		// If the profession blacklist contains this profession, run the default functionality.
		String iconName = Markers.getIconName(entity);
		if (MerchantMarkersConfig.INSTANCE.professionBlacklist.get().contains(iconName))
		{
			EntityIconDefinitions.buildVariantIdString(stringBuilder, entityRenderer, entity);
		}
		// Otherwise, we'll fill in a standin identifier for a dynamically-populated icon (see below).
		else
		{
			stringBuilder.append(iconName);
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
		if (XaeroMinimap.instance.getInterfaces() != null)
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

		ResourceManager manager = Minecraft.getInstance().getResourceManager();

		BufferedImage newImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = newImage.createGraphics();
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		// Maybe it's just not loaded yet?  Bail for now.
		if (!manager.hasResource(resource.texture()) && Minecraft.getInstance().getTextureManager().getTexture(resource.texture(), null) == null)
		{
			return InputStream.nullInputStream();
		}

		try
		{
			// Lazy-load the overlay image now if needed.
			if (overlayImage == null)
			{
				overlayImage = ImageIO.read(manager.getResource(Markers.ICON_OVERLAY).getInputStream());
			}

			BufferedImage originalImage = ImageIO.read(manager.getResource(resource.texture()).getInputStream());

			// TODO: Configuration option for this?
			final int size = 32;

			// Draw the icon centered in the new image.
			graphics.drawImage(originalImage, (64 - size) / 2, (64 + size) / 2, (64 + size) / 2, (64 - size) / 2,
							   0, 0, (int)(originalImage.getWidth()), (int)(originalImage.getHeight()), null);

			// Also draw the overlay graphic.
			int overlayIndex = MerchantMarkersConfig.INSTANCE.overlayIndex.get();
			if (overlayIndex != -1 && resource.overlay())
			{
				graphics.drawImage(overlayImage, 32, 32, (64 + size) / 2, (64 - size) / 2,
				(overlayIndex % 2) * (int)(overlayImage.getWidth() / 2),
				(overlayIndex / 2) * (int)(overlayImage.getHeight() / 2),
				(overlayIndex % 2) * (int)(overlayImage.getWidth() / 2) + (int)(overlayImage.getWidth() / 2),
				(overlayIndex / 2) * (int)(overlayImage.getHeight() / 2) + (int)(overlayImage.getHeight() / 2), null);
			}

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
		Minecraft mc = Minecraft.getInstance();
		ResourceManager manager = mc.getResourceManager();

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
				dynamicPack.registerResource(PackType.CLIENT_RESOURCES, new ResourceLocation("xaerominimap", "entity/icon/definition/minecraft/villager.json"), () -> {

					// Dynamically build the .json file to include all current villager markers available, with dynamic proxies for each.
					JsonObject variants = new JsonObject();
					for (ResourceLocation marker : delayedResources.get())
					{
						String[] components = marker.getPath().split("/");
						String iconName = components[components.length - 1].replace(".png", "");
						variants.addProperty(iconName, "sprite:" + marker.getPath());
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

					ResourceLocation markerLocation = new ResourceLocation("xaerominimap", "entity/icon/sprite/" + marker.getPath());

					// If this location is already registered in Minecraft's texture manager, release it first.
					if (mc.getTextureManager().getTexture(markerLocation, null) != null)
					{
						mc.execute(() -> mc.getTextureManager().release(markerLocation));
					}

					// Register a proxy resource to display our chosen icon.
					dynamicPack.registerResource(PackType.CLIENT_RESOURCES, markerLocation, () -> {
						try
						{
							InputStream proxyStream = getResizedIcon(() -> Markers.getMarkerResource(mc, iconName));
							if (proxyStream.available() == 0)
							{
								return reloadableManager.getResource(Markers.getMarkerResource(mc, iconName).texture()).getInputStream();
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
