package com.anthonyhilyard.merchantmarkers;

import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;
import com.anthonyhilyard.iceberg.services.Services;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class MerchantMarkers
{
	public static final String MODID = "merchantmarkers";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static boolean comparisonsActive = false;
	public static final KeyMapping showMarkers = Services.getKeyMappingRegistrar().registerMapping(new KeyMapping("merchantmarkers.key.showMarkers",
										InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "merchantmarkers.key.categories.merchantMarkers"));

	public static void init()
	{
		MerchantMarkersConfig.register(MerchantMarkersConfig.class, MODID);
	}
}