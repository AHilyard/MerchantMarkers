package com.anthonyhilyard.merchantmarkers.fabric.client;

import com.anthonyhilyard.merchantmarkers.client.MerchantMarkersClient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class MerchantMarkersFabricClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			MerchantMarkersClient.init();
		});
	}
}
