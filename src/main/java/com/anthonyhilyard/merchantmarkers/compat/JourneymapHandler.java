package com.anthonyhilyard.merchantmarkers.compat;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.render.Markers;

import org.apache.commons.lang3.StringUtils;

import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.forge.EntityRadarUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;

@ClientPlugin
public class JourneymapHandler implements IClientPlugin
{

	@Override
	public void initialize(IClientAPI iClientAPI)
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public String getModId()
	{
		return Loader.MODID;
	}

	@Override
	public void onEvent(ClientEvent clientEvent) { }

	@SubscribeEvent
	public void onEntityUpdate(EntityRadarUpdateEvent event)
	{
		// If we are showing custom icons on the minimap, replace the standard JourneyMap icons now.
		if (MerchantMarkersConfig.getInstance().showOnMiniMap.get())
		{
			LivingEntity entity = event.getWrappedEntity().getEntityLivingRef().get();

			// If this entity is marker-able, update the texture before drawing.
			if (Markers.shouldShowMarker(entity))
			{
				String profession = Markers.getProfessionName(entity);

				// Return the default texture for blacklisted professions.
				if (!MerchantMarkersConfig.getInstance().professionBlacklist.get().contains(profession))
				{
					final Minecraft mc = Minecraft.getInstance();
					int level = Markers.getProfessionLevel(entity);
					event.getWrappedEntity().setEntityIconLocation(Markers.getMarkerResource(mc, profession, level).texture());

					// Let's give them a fun mouseover tooltip!
					String formattedProfession = StringUtils.capitalize(profession.replace("_", " ").toLowerCase());
					event.getWrappedEntity().setEntityToolTips(Collections.singletonList(Component.literal(formattedProfession)));
				}
			}
		}
	}
}