package com.anthonyhilyard.merchantmarkers.compat;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.render.Markers;

import org.apache.commons.lang3.StringUtils;

import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.fabric.EntityRadarUpdateEvent;
import journeymap.client.api.event.fabric.FabricEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;

import java.util.Collections;

public class JourneymapHandler implements IClientPlugin
{
	@Override
	public void initialize(IClientAPI jmClientApi)
	{
		FabricEvents.ENTITY_RADAR_UPDATE_EVENT.register(this::onEntityUpdate);
	}

	@Override
	public String getModId()
	{
		return Loader.MODID;
	}

	@Override
	public void onEvent(ClientEvent clientEvent) { }

	@SuppressWarnings("deprecation")
	public void onEntityUpdate(EntityRadarUpdateEvent event)
	{
		// If we are showing custom icons on the minimap, replace the standard JourneyMap icons now.
		if (MerchantMarkersConfig.INSTANCE.showOnMiniMap.get())
		{
			LivingEntity entity = event.getWrappedEntity().getEntityLivingRef().get();

			// If this entity is marker-able, update the texture before drawing.
			if (Markers.shouldShowMarker(entity))
			{
				String profession = Markers.getProfessionName(entity);

				// Return the default texture for blacklisted professions.
				if (!MerchantMarkersConfig.INSTANCE.professionBlacklist.get().contains(profession))
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