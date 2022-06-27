package com.anthonyhilyard.merchantmarkers;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class MerchantMarkers implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		ModLoadingContext.registerConfig(Loader.MODID, ModConfig.Type.COMMON, MerchantMarkersConfig.SPEC);

		ClientLifecycleEvents.CLIENT_STARTED.register((client) ->
		{
			try
			{
				// If Xaero's minimap is loaded, add a resource manager listener for dynamically-sized map icons.
				if (FabricLoader.getInstance().isModLoaded("xaerominimap"))
				{
					Class.forName("com.anthonyhilyard.merchantmarkers.compat.XaeroMinimapHandler").getMethod("setupDynamicIcons").invoke(null);
				}
				// Same thing for FTB Chunks.
				if (FabricLoader.getInstance().isModLoaded("ftbchunks"))
				{
					Class.forName("com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler").getMethod("setupDynamicIcons").invoke(null);
				}
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e.getCause() == null ? e : e.getCause()));
			}
		});
		
	}
}
