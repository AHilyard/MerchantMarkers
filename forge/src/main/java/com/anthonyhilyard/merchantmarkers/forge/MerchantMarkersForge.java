package com.anthonyhilyard.merchantmarkers.forge;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(MerchantMarkers.MODID)
public final class MerchantMarkersForge
{
	public MerchantMarkersForge(ModLoadingContext context)
	{
		context.registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true));
	}
}
