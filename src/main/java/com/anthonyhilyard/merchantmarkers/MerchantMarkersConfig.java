package com.anthonyhilyard.merchantmarkers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.Map.entry;

import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.api.fml.event.config.ModConfigEvent;

public class MerchantMarkersConfig
{
	public static final ForgeConfigSpec SPEC;
	public static final MerchantMarkersConfig INSTANCE;

	public final BooleanValue alwaysShow;
	public final BooleanValue showThroughWalls;
	public final BooleanValue showArrow;
	public final BooleanValue showOnMiniMap;
	public final IntValue overlayIndex;
	public final DoubleValue opacity;
	public final DoubleValue maxDistance;
	public final DoubleValue fadePercent;
	public final DoubleValue iconScale;
	public final DoubleValue minimapIconScale;
	public final IntValue verticalOffset;

	public final ConfigValue<? extends String> markerType;
	public final ConfigValue<Config> associatedItems;
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

	public enum OverlayType
	{
		NONE(-1),
		BACKPACK(0),
		EMERALD(1),
		COINS(2),
		BAG(3),
		LEVEL(4);

		private final int value;
		private OverlayType(int value) { this.value = value; }

		public int value() { return value; }

		public static Optional<OverlayType> fromValue(int value)
		{
			return Arrays.stream(values()).filter(v -> v.value == value).findFirst();
		}
	}

	private static Map<String, String> defaultAssociatedItems = new LinkedHashMap<String, String>(Map.ofEntries(
		entry("armorer",			"minecraft:iron_chestplate"),
		entry("butcher",			"minecraft:beef"),
		entry("cartographer",		"minecraft:compass"),
		entry("cleric",				"minecraft:rotten_flesh"),
		entry("farmer",				"minecraft:wheat"),
		entry("fisherman",			"minecraft:cod"),
		entry("fletcher",			"minecraft:bow"),
		entry("leatherworker",		"minecraft:leather"),
		entry("librarian",			"minecraft:bookshelf"),
		entry("mason",				"minecraft:brick"),
		entry("shepherd",			"minecraft:shears"),
		entry("toolsmith",			"minecraft:iron_pickaxe"),
		entry("weaponsmith",		"minecraft:iron_sword"),
		entry("wandering_trader",	"minecraft:emerald")
	));

	static
	{
		Config.setInsertionOrderPreserved(true);
		Pair<MerchantMarkersConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(MerchantMarkersConfig::new);
		SPEC = specPair.getRight();
		INSTANCE = specPair.getLeft();
	}

	public MerchantMarkersConfig(ForgeConfigSpec.Builder build)
	{
		build.comment("Client Configuration").push("client").push("visual_options");

		alwaysShow = build.comment(" If markers above villagers should always show. If false, they will only show when the configured keybind is held.").define("always_show", true);
		showThroughWalls = build.comment(" If markers should be visible through walls and other obstructions.").define("show_through_walls", true);
		showArrow = build.comment(" If markers should include an arrow under the profession-specific icon.").define("show_arrow", true);
		showOnMiniMap = build.comment(" If icons should show on minimaps. (Currently supports Xaero's Minimap, FTB Chunks, and JourneyMap).").define("show_on_minimap", true);
		overlayIndex = build.comment(" Which overlay graphic to use (0 = backpack, 1 = emerald, 2 = coin stack, 3 = bag, 4 = profession level, -1 = none).").defineInRange("overlay_icon", 3, -1, 4);
		opacity = build.comment(" The opacity of displayed markers and arrows.").defineInRange("opacity", 1.0, 0.1, 1.0);
		maxDistance = build.comment(" The maximum distance, in blocks, at which markers are visible.").defineInRange("max_distance", 64.0, 16.0, 256.0);
		fadePercent = build.comment(" The percent of the maximum distance at which markers will begin to fade out.").defineInRange("fade_percent", 25.0, 0.0, 100.0);
		iconScale = build.comment(" How large in-world markers should appear.").defineInRange("icon_scale", 1.0, 0.5, 2.0);
		minimapIconScale = build.comment(" How large markers should appear on minimaps. (Only applicable for maps without a built-in icon scale option.)").defineInRange("minimap_icon_scale", 0.75, 0.5, 2.0);
		verticalOffset = build.comment(" How high above villagers markers should appear.  The default position (0) is right above name plates.").defineInRange("vertical_offset", 0, -128, 128);
		markerType = build.comment(" The types of markers to show above villagers.  Can be one of either \"items\", \"jobs\", \"generic\", or \"custom\".  These options mean:\n" +
								   "    \"items\" - Shows items from the associated item list below.\n" +
								   "    \"jobs\" - Shows the texture from the villager's job site block (like the brewing stand for clerics, and so on).\n" +
								   "    \"generic\" - Shows a generic icon that is the same for all villagers.\n" +
								   "    \"custom\" - Shows custom icons for each villager profession (these can be replaced with a resource pack).")
								   .defineInList("marker_type", "custom", Arrays.stream(MarkerType.values()).map(v -> v.name().toLowerCase()).toList());

		professionBlacklist = build.comment(" A list of professions to ignore when displaying markers. Use \"none\" for villagers with no profession.").define("profession_blacklist", Lists.newArrayList("none", "nitwit"));
		associatedItems = build.comment(" The items associated with each villager profession.  Only used when marker type is set to \"items\".\n If not specified here, vanilla professions will have a default item and modded professions will have a generic icon.").define("associated_items", Config.of(TomlFormat.instance()), (v) -> validateAssociatedItems((Config)v));

		build.pop().pop();

		ModConfigEvent.RELOADING.register(MerchantMarkersConfig::onLoad);
	}

	/**
	 * Helper function that returns true if we are showing profession levels.
	 */
	public boolean showLevels()
	{
		return OverlayType.LEVEL.equals(OverlayType.fromValue(overlayIndex.get()).orElse(null));
	}

	public ResourceLocation getAssociatedItem(String profession)
	{
		Map<String, Object> configuredItems = associatedItems.get().valueMap();
		if (configuredItems.containsKey(profession) && 
			configuredItems.get(profession) instanceof String &&
			ResourceLocation.isValidResourceLocation((String)configuredItems.get(profession)))
		{
			return new ResourceLocation((String)configuredItems.get(profession));
		}

		if (defaultAssociatedItems.containsKey(profession))
		{
			return new ResourceLocation(defaultAssociatedItems.get(profession));
		}

		return null;
	}

	private static boolean validateAssociatedItems(Config v)
	{
		if (v == null || v.valueMap() == null)
		{
			return false;
		}

		// Note that if there is a non-null config value, this validation function always returns true because the entire collection is cleared otherwise, which sucks.
		for (String key : v.valueMap().keySet())
		{
			Object value = v.valueMap().get(key);

			// Value must be a string and a valid resource location.
			if (!(value instanceof String) || !ResourceLocation.isValidResourceLocation((String)value))
			{
				Loader.LOGGER.warn("Invalid associated item found: \"{}\".  This value was ignored.", value);
			}
		}

		return true;
	}

	public static void onLoad(ModConfig config)
	{
		if (config.getModId().equals(Loader.MODID))
		{
			Markers.clearResourceCache();
			try
			{
				if (FabricLoader.getInstance().isModLoaded("xaerominimap"))
				{
					Class.forName("com.anthonyhilyard.merchantmarkers.compat.XaeroMinimapHandler").getMethod("clearIconCache").invoke(null);
				}
				if (FabricLoader.getInstance().isModLoaded("ftbchunks"))
				{
					Class.forName("com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler").getMethod("clearIconCache").invoke(null);
				}
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}
	}

}
