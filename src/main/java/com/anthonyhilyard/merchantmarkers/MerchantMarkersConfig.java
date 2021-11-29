package com.anthonyhilyard.merchantmarkers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber(modid = Loader.MODID, bus = Bus.MOD)
public class MerchantMarkersConfig
{
	public static final ForgeConfigSpec SPEC;
	public static final MerchantMarkersConfig INSTANCE;

	public final BooleanValue showThroughWalls;
	public final BooleanValue showArrow;
	public final BooleanValue showOnMiniMap;
	public final IntValue overlayIndex;
	public final DoubleValue maxDistance;
	public final DoubleValue fadePercent;

	public final ConfigValue<? extends String> markerType;

	public final ConfigValue<CommentedConfig> associatedItems;

	public final ConfigValue<List<? extends String>> professionBlacklist;

	public enum MarkerType
	{
		ITEMS,
		JOBS,
		GENERIC,
		CUSTOM;

		public static Optional<MarkerType> fromText(String text)
		{
			return Arrays.stream(values()).filter(v -> v.name().equalsIgnoreCase(text)).findFirst();
		}
	}

	private static Supplier<CommentedConfig> defaultAssociatedItems = null;

	static
	{
		defaultAssociatedItems = () -> TomlFormat.newConfig(() -> new LinkedHashMap<String, Object>() {{
			put("armorer",			"minecraft:iron_chestplate");
			put("butcher",			"minecraft:beef");
			put("cartographer",		"minecraft:compass");
			put("cleric",				"minecraft:rotten_flesh");
			put("farmer",				"minecraft:wheat");
			put("fisherman",			"minecraft:cod");
			put("fletcher",			"minecraft:bow");
			put("leatherworker",		"minecraft:leather");
			put("librarian",			"minecraft:bookshelf");
			put("mason",				"minecraft:brick");
			put("shepherd",			"minecraft:shears");
			put("toolsmith",			"minecraft:iron_pickaxe");
			put("weaponsmith",		"minecraft:iron_sword");
			put("wandering_trader",	"minecraft:emerald");
		}});

		Config.setInsertionOrderPreserved(true);
		Pair<MerchantMarkersConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(MerchantMarkersConfig::new);
		SPEC = specPair.getRight();
		INSTANCE = specPair.getLeft();
	}

	public MerchantMarkersConfig(ForgeConfigSpec.Builder build)
	{
		build.comment("Client Configuration").push("client").push("visual_options");

		showThroughWalls = build.comment(" If markers should be visible through walls and other obstructions.").define("show_through_walls", true);
		showArrow = build.comment(" If markers should include an arrow under the profession-specific icon.").define("show_arrow", true);
		showOnMiniMap = build.comment(" If icons should show on minimaps. (Currently supports Xaero's Minimap).").define("show_on_minimap", true);
		overlayIndex = build.comment(" Which overlay graphic to use (0 = backpack, 1 = emerald, 2 = coin stack, 3 = bag, -1 = none).").defineInRange("overlay_icon", 3, -1, 3);
		maxDistance = build.comment(" The maximum distance, in blocks, at which markers are visible.").defineInRange("max_distance", 64.0, 16.0, 256.0);
		fadePercent = build.comment(" The percent of the maximum distance at which markers will begin to fade out.").defineInRange("fade_percent", 25.0, 0.0, 100.0);
		markerType = build.comment(" The types of markers to show above villagers.  Can be one of either \"items\", \"jobs\", \"generic\", or \"custom\".  These options mean:\n" +
								   "    \"items\" - Shows items from the associated item list below.\n" +
								   "    \"jobs\" - Shows the texture from the villager's job site block (like the brewing stand for clerics, and so on).\n" +
								   "    \"generic\" - Shows a generic icon that is the same for all villagers.\n" +
								   "    \"custom\" - Shows custom icons for each villager profession (these can be replaced with a resource pack).")
								   .defineInList("marker_type", "custom", Arrays.stream(MarkerType.values()).map(v -> v.name().toLowerCase()).collect(Collectors.toList()));

		professionBlacklist = build.comment(" A list of professions to ignore when displaying markers. Use \"none\" for villagers with no profession.").define("profession_blacklist", Arrays.asList("none", "nitwit"));
		associatedItems = build.comment(" The items associated with each villager profession.  Only used when marker type is set to \"items\".").define(Arrays.asList("associated_items"), defaultAssociatedItems, (v) -> true, CommentedConfig.class);

		build.pop().pop();
	}

	@SubscribeEvent
	public static void onLoad(ModConfig.Reloading event)
	{
		if (event.getConfig().getModId().equals(Loader.MODID))
		{
			Loader.LOGGER.info("Merchant Markers config reloaded.");
			Markers.clearResourceCache();
			if (ModList.get().isLoaded("xaerominimap"))
			{
				try
				{
					Class.forName("com.anthonyhilyard.merchantmarkers.XaeroHandler").getMethod("clearIconCache").invoke(null);
				}
				catch (Exception e)
				{
					Loader.LOGGER.error(e.toString());
				}
			}
		}
	}

}