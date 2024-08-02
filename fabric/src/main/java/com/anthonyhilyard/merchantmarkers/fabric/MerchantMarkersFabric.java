package com.anthonyhilyard.merchantmarkers.fabric;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;

import net.fabricmc.api.ModInitializer;

public final class MerchantMarkersFabric implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		MerchantMarkers.init();
	}
}
