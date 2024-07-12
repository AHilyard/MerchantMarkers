package com.anthonyhilyard.merchantmarkers.client;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.iceberg.services.Services;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class MerchantMarkersClient
{
	public static void init()
	{
		try
		{
			// If Xaero's minimap is loaded, add a resource manager listener for dynamically-sized map icons.
			if (Services.PLATFORM.isModLoaded("xaerominimap"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.XaeroMinimapHandler").getMethod("setupDynamicIcons").invoke(null);
			}
			// Same thing for FTB Chunks.
			if (Services.PLATFORM.isModLoaded("ftbchunks"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler").getMethod("setupDynamicIcons").invoke(null);
			}
		}
		catch (Exception e)
		{
			MerchantMarkers.LOGGER.error(ExceptionUtils.getStackTrace(e.getCause() == null ? e : e.getCause()));
		}
	}
}