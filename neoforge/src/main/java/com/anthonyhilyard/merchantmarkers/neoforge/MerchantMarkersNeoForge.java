package com.anthonyhilyard.merchantmarkers.neoforge;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.neoforge.client.MerchantMarkersNeoForgeClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(MerchantMarkers.MODID)
public final class MerchantMarkersNeoForge
{
	public MerchantMarkersNeoForge(ModContainer container, IEventBus modBus)
	{
		// Run our common setup.
		MerchantMarkers.init();

		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			modBus.register(MerchantMarkersNeoForgeClient.class);
		}
	}
}

