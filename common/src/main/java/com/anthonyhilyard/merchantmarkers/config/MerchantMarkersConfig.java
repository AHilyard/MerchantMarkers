package com.anthonyhilyard.merchantmarkers.config;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Map.entry;

import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.services.IIcebergConfigSpecBuilder;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.merchantmarkers.MerchantMarkers;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.resources.ResourceLocation;


public class MerchantMarkersConfig extends IcebergConfig<MerchantMarkersConfig>
{
	public static MerchantMarkersConfig getInstance() { return (MerchantMarkersConfig)configInstances.get(MerchantMarkers.MODID); }

	public final Supplier<Boolean> alwaysShow;
	public final Supplier<Boolean> showThroughWalls;
	public final Supplier<Boolean> showArrow;
	public final Supplier<Boolean> showOnMiniMap;
	public final Supplier<Integer> overlayIndex;
	public final Supplier<Double> opacity;
	public final Supplier<Double> maxDistance;
	public final Supplier<Double> fadePercent;
	public final Supplier<Double> iconScale;
	public final Supplier<Double> minimapIconScale;
	public final Supplier<Integer> verticalOffset;

	public final Supplier<? extends String> markerType;
	public final Supplier<Map<String, Object>> associatedItems;
	public final Supplier<List<? extends String>> professionBlacklist;

	public final Supplier<Boolean> enableOptifineWorkaround;
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

	private static Map<String, Object> defaultAssociatedItems = new LinkedHashMap<String, Object>(Map.ofEntries(
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

	public MerchantMarkersConfig(IIcebergConfigSpecBuilder build)
	{
		build.comment("Client Configuration").push("client").push("visual_options");

		alwaysShow = build.comment(" If markers above villagers should always show. If false, they will only show when the configured keybind is held.").add("always_show", true);
		showThroughWalls = build.comment(" If markers should be visible through walls and other obstructions.").add("show_through_walls", true);
		showArrow = build.comment(" If markers should include an arrow under the profession-specific icon.").add("show_arrow", true);
		showOnMiniMap = build.comment(" If icons should show on minimaps. (Currently supports Xaero's Minimap, FTB Chunks, and JourneyMap).").add("show_on_minimap", true);
		overlayIndex = build.comment(" Which overlay graphic to use (0 = backpack, 1 = emerald, 2 = coin stack, 3 = bag, 4 = profession level, -1 = none).").addInRange("overlay_icon", 3, -1, 4);
		opacity = build.comment(" The opacity of displayed markers and arrows.").addInRange("opacity", 1.0, 0.1, 1.0);
		maxDistance = build.comment(" The maximum distance, in blocks, at which markers are visible.").addInRange("max_distance", 64.0, 16.0, 256.0);
		fadePercent = build.comment(" The percent of the maximum distance at which markers will begin to fade out.").addInRange("fade_percent", 25.0, 0.0, 100.0);
		iconScale = build.comment(" How large in-world markers should appear.").addInRange("icon_scale", 1.0, 0.5, 2.0);
		minimapIconScale = build.comment(" How large markers should appear on minimaps. (Only applicable for maps without a built-in icon scale option.)").addInRange("minimap_icon_scale", 0.75, 0.5, 2.0);
		verticalOffset = build.comment(" How high above villagers markers should appear.  The default position (0) is right above name plates.").addInRange("vertical_offset", 0, -128, 128);
		markerType = build.comment(" The types of markers to show above villagers.  Can be one of either \"items\", \"jobs\", \"generic\", or \"custom\".  These options mean:\n" +
								   "    \"items\" - Shows items from the associated item list below.\n" +
								   "    \"jobs\" - Shows the texture from the villager's job site block (like the brewing stand for clerics, and so on).\n" +
								   "    \"generic\" - Shows a generic icon that is the same for all villagers.\n" +
								   "    \"custom\" - Shows custom icons for each villager profession (these can be replaced with a resource pack).")
								   .addInList("marker_type", "custom", Arrays.stream(MarkerType.values()).map(v -> v.name().toLowerCase()).toList());

		professionBlacklist = build.comment(" A list of professions to ignore when displaying markers. Use \"none\" for villagers with no profession.").add("profession_blacklist", Lists.newArrayList("none", "nitwit"));
		associatedItems = build.comment(" The items associated with each villager profession.  Only used when marker type is set to \"items\".\n If not specified here, vanilla professions will have a default item and modded professions will have a generic icon.").addSubconfig("associated_items", defaultAssociatedItems, (k) -> k != null && k instanceof String str && !str.isEmpty(), (v) -> validateAssociatedItems(v));

		build.pop().push("compatibility_options");

		enableOptifineWorkaround = build.comment(" If enabled, will force fast render on when using shaders with Optifine (due to a bug in Optifine, this is required for markers to render properly with some shaders).").add("optifine_workaround", true);

		build.pop().pop();
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
		Map<String, Object> configuredItems = associatedItems.get();
		ResourceLocation resourceLocation = ResourceLocation.tryParse((String)configuredItems.get(profession));
		if (configuredItems.containsKey(profession) && 
			configuredItems.get(profession) instanceof String &&
			resourceLocation != null)
		{
			return resourceLocation;
		}

		if (defaultAssociatedItems.containsKey(profession))
		{
			return ResourceLocation.tryParse((String)defaultAssociatedItems.get(profession));
		}

		return null;
	}

	private static boolean validateAssociatedItems(Object value)
	{
		// Value must be a string and a valid resource location.
		if (!(value instanceof String str) || ResourceLocation.tryParse(str) == null)
		{
			return false;
		}

		return true;
	}

	@Override
	protected void onReload()
	{
		Markers.clearResourceCache();
		try
		{
			if (Services.getPlatformHelper().isModLoaded("xaerominimap"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.XaeroMinimapHandler").getMethod("clearIconCache").invoke(null);
			}
			if (Services.getPlatformHelper().isModLoaded("ftbchunks"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler").getMethod("clearIconCache").invoke(null);
			}
		}
		catch (Exception e)
		{
			MerchantMarkers.LOGGER.error(ExceptionUtils.getStackTrace(e));
		}
	}

}
