package com.anthonyhilyard.merchantmarkers;

import com.anthonyhilyard.merchantmarkers.render.Markers;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class MerchantMarkers
{
	public void onClientSetup(FMLClientSetupEvent event)
	{
		try
		{
			// If Xaero's minimap is loaded, add a resource manager listener for dynamically-sized map icons.
			if (ModList.get().isLoaded("xaerominimap"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.XaeroHandler").getMethod("setupDynamicIcons").invoke(null);
			}
			// Same thing for FTB Chunks.
			if (ModList.get().isLoaded("ftbchunks"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.FTBChunksHandler").getMethod("setupDynamicIcons").invoke(null);
			}
		}
		catch (Exception e)
		{
			Loader.LOGGER.error(ExceptionUtils.getStackTrace(e.getCause()));
		}
	}

	@SubscribeEvent
	public static void onRenderNameplate(RenderNameplateEvent event)
	{
		Markers.renderMarker(event.getEntityRenderer(), event.getEntity(), event.getContent(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
	}
}
