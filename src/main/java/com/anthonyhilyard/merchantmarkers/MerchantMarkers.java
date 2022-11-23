package com.anthonyhilyard.merchantmarkers;

import com.anthonyhilyard.merchantmarkers.render.Markers;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class MerchantMarkers
{
	private static final KeyBinding showMarkers = new KeyBinding("merchantmarkers.key.showMarkers", KeyConflictContext.IN_GAME, InputMappings.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT), "merchantmarkers.key.categories.merchantMarkers");

	public void onClientSetup(FMLClientSetupEvent event)
	{
		ClientRegistry.registerKeyBinding(showMarkers);

		try
		{
			// If Xaero's minimap is loaded, add a resource manager listener for dynamically-sized map icons.
			if (ModList.get().isLoaded("xaerominimap"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.XaeroMinimapHandler").getMethod("setupDynamicIcons").invoke(null);
			}
			// Same thing for FTB Chunks.
			if (ModList.get().isLoaded("ftbchunks"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler").getMethod("setupDynamicIcons").invoke(null);
			}
		}
		catch (Exception e)
		{
			Loader.LOGGER.error(ExceptionUtils.getStackTrace(e.getCause() == null ? e : e.getCause()));
		}
	}

	@SubscribeEvent
	public static void onRenderNameplate(RenderNameplateEvent event)
	{
		if (showMarkers.isDown() || MerchantMarkersConfig.INSTANCE.alwaysShow.get())
		{
			Markers.renderMarker(event.getEntityRenderer(), event.getEntity(), event.getContent(), event.getMatrixStack(), event.getRenderTypeBuffer(), event.getPackedLight());
		}
	}
}
