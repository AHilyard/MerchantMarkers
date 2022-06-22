package com.anthonyhilyard.merchantmarkers.compat;

import java.lang.reflect.Field;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class OptifineHandler implements ResourceManagerReloadListener
{
	private static OptifineHandler INSTANCE = new OptifineHandler();
	private static Minecraft minecraft;
	private static Boolean installed = null;

	public static void init()
	{
		minecraft = Minecraft.getInstance();

		fixRenderSettings();
		if (minecraft.getResourceManager() instanceof ReloadableResourceManager resourceManager)
		{
			resourceManager.registerReloadListener(INSTANCE);
		}
	}

	public static boolean optifineInstalled()
	{
		if (installed == null)
		{
			installed = false;
			try
			{
				Class.forName("net.optifine.shaders.Shaders", false, OptifineHandler.class.getClassLoader());
				installed = true;
			}
			catch (Exception e) {}
		}

		return installed;
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager)
	{
		fixRenderSettings();
	}

	private static void fixRenderSettings()
	{
		// Don't do anything if the optifine workaround is disabled.
		if (!MerchantMarkersConfig.getInstance().enableOptifineWorkaround.get())
		{
			return;
		}

		try
		{
			// Check to see if a shaderpack is enabled.
			String shaderPackName = (String)Class.forName("net.optifine.shaders.Shaders").getMethod("getShaderPackName").invoke(null);
			
			// Grab the fast render option to see if it is set.
			Field fastRenderField = Class.forName("net.minecraft.client.Options").getField("ofFastRender");
			Boolean fastRender = (Boolean)fastRenderField.get(minecraft.options);

			// If a shader pack is loaded and fast render is disabled, turn on fast render.
			if (shaderPackName != null && !fastRender)
			{
				fastRenderField.set(minecraft.options, true);
				minecraft.options.save();

				String warningMessage = I18n.get("merchantmarkers.general.optifine_workaround_warning");

				// Output a message to the user so they know the setting was changed.
				if (minecraft.player != null)
				{
					minecraft.player.sendSystemMessage(Component.literal(warningMessage).withStyle(ChatFormatting.GOLD));
				}
				else
				{
					Loader.LOGGER.warn(warningMessage);
				}
			}
			
		}
		catch (Exception e)
		{
			Loader.LOGGER.error(ExceptionUtils.getStackTrace(e.getCause() == null ? e : e.getCause()));
		}
	}
}
