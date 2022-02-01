package com.anthonyhilyard.merchantmarkers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Loader.MODID)
public class Loader
{
	public static final String MODID = "merchantmarkers";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public Loader()
	{
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			if (!MerchantMarkersConfig.register(MerchantMarkersConfig.class, MODID))
			{
				LOGGER.error("Failed to register configuration for Merchant Markers!");
			}
			MerchantMarkers mod = new MerchantMarkers();
			FMLJavaModLoadingContext.get().getModEventBus().addListener(mod::onClientSetup);
			MinecraftForge.EVENT_BUS.register(MerchantMarkers.class);
		}
	}
}