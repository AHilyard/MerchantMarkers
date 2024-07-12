package com.anthonyhilyard.merchantmarkers.neoforge.client;

import com.anthonyhilyard.merchantmarkers.client.MerchantMarkersClient;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public class MerchantMarkersNeoForgeClient
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		MerchantMarkersClient.init();
	}
}
