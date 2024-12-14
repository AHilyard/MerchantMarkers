package com.anthonyhilyard.merchantmarkers.forge;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MerchantMarkers.MODID)
public final class MerchantMarkersForge
{
	public MerchantMarkersForge(FMLJavaModLoadingContext context)
	{
		context.registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true));
	}
}
