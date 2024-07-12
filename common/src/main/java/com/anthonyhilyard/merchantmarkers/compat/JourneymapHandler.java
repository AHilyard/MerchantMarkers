package com.anthonyhilyard.merchantmarkers.compat;

import java.util.Collections;

import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.config.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.render.Markers;

import org.apache.commons.lang3.StringUtils;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.client.event.EntityRadarUpdateEvent;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

@JourneyMapPlugin(apiVersion = "2.0")
public class JourneymapHandler implements IClientPlugin
{
	public JourneymapHandler() {}

	@Override
	public void initialize(IClientAPI jmClientApi)
	{
		ClientEventRegistry.ENTITY_RADAR_UPDATE_EVENT.subscribe(getModId(), this::onEntityUpdate);
	}

	@Override
	public String getModId()
	{
		return MerchantMarkers.MODID;
	}

	public void onEntityUpdate(EntityRadarUpdateEvent event)
	{
		// If we are showing custom icons on the minimap, replace the standard JourneyMap icons now.
		if (MerchantMarkersConfig.getInstance().showOnMiniMap.get())
		{
			Entity entity = event.getWrappedEntity().getEntityRef().get();

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