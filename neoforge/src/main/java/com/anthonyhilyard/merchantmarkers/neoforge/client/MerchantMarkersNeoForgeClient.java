package com.anthonyhilyard.merchantmarkers.neoforge.client;

import com.anthonyhilyard.merchantmarkers.client.MerchantMarkersClient;
import com.anthonyhilyard.merchantmarkers.MerchantMarkers;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;


@Mod(value = MerchantMarkers.MODID, dist = Dist.CLIENT)
public class MerchantMarkersNeoForgeClient
{
	public MerchantMarkersNeoForgeClient(IEventBus modBus)
	{
		MerchantMarkers.init();

		modBus.register(MerchantMarkersNeoForgeClient.class);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		MerchantMarkersClient.init();
	}
}
