package com.anthonyhilyard.merchantmarkers.forge;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.forge.client.MerchantMarkersForgeClient;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MerchantMarkers.MODID)
public final class MerchantMarkersForge
{
	public MerchantMarkersForge()
	{
		// Run our common setup.
		MerchantMarkers.init();

		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			MinecraftForge.EVENT_BUS.register(MerchantMarkersForgeClient.class);
		}

		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true));
	}
}
