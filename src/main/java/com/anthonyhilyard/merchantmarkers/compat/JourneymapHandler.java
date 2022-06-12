package com.anthonyhilyard.merchantmarkers.compat;

import com.anthonyhilyard.merchantmarkers.Loader;
import com.anthonyhilyard.merchantmarkers.MerchantMarkersConfig;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.forge.EntityRadarUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.util.text.StringTextComponent;
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
    public void onEvent(ClientEvent clientEvent)
    {

    }

    @SubscribeEvent
    public void onEntityUpdate(EntityRadarUpdateEvent event)
    {
        LivingEntity entity = event.getWrappedEntity().getEntityLivingRef().get();

        // If this entity is marker-able, update the texture before drawing.
        if (entity instanceof AbstractVillagerEntity && !(entity).isBaby())
        {
            String profession = Markers.getProfessionName(entity);

            // Return the default texture for blacklisted professions.
            if (!MerchantMarkersConfig.INSTANCE.professionBlacklist.get().contains(profession))
            {
                int level = Markers.getProfessionLevel(entity);
                event.getWrappedEntity().setEntityIconLocation(Markers.getMarkerResource(Minecraft.getInstance(), profession, level).texture);
                // let's give them a fun mouseover tooltip!
                event.getWrappedEntity().setEntityToolTips(Collections.singletonList(new StringTextComponent(profession)));
            }
        }
    }
}
