package com.anthonyhilyard.merchantmarkers.forge.client;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.client.MerchantMarkersClient;
import com.anthonyhilyard.merchantmarkers.forge.compat.OptifineHandler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;


@EventBusSubscriber(modid = MerchantMarkers.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class MerchantMarkersForgeClient
{
	@SubscribeEvent
	public static void onConstructMod(final FMLConstructModEvent event)
	{
		MerchantMarkers.init();
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onClientSetup(FMLClientSetupEvent event)
	{
		MerchantMarkersClient.init();

		// If optifine is installed, we have to do some hacks to ensure it doesn't break markers.
		if (OptifineHandler.optifineInstalled())
		{
			MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent serverStartedEvent) -> { OptifineHandler.init(); });
		}
	}
}
