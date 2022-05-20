package com.anthonyhilyard.merchantmarkers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.anthonyhilyard.iceberg.config.IcebergConfig;
import com.anthonyhilyard.iceberg.config.IcebergConfigSpec;
import com.anthonyhilyard.merchantmarkers.render.Markers;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.ModList;

public class MerchantMarkersConfig extends IcebergConfig<MerchantMarkersConfig>
{
	private static MerchantMarkersConfig INSTANCE;
	public static MerchantMarkersConfig getInstance() { return INSTANCE; }

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
	public final ConfigValue<UnmodifiableConfig> associatedItems;
	public final ConfigValue<List<? extends String>> professionBlacklist;

	public final BooleanValue enableOptifineWorkaround;

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

	private static final UnmodifiableConfig defaultAssociatedItems;

	static
	{
		ForgeConfigSpec.Builder defaultAssociatedItemsBuilder = new ForgeConfigSpec.Builder();
		defaultAssociatedItemsBuilder.define("armorer",				"minecraft:iron_chestplate");
		defaultAssociatedItemsBuilder.define("butcher",				"minecraft:beef");
		defaultAssociatedItemsBuilder.define("cartographer",		"minecraft:compass");
		defaultAssociatedItemsBuilder.define("cleric",				"minecraft:rotten_flesh");
		defaultAssociatedItemsBuilder.define("farmer",				"minecraft:wheat");
		defaultAssociatedItemsBuilder.define("fisherman",			"minecraft:cod");
		defaultAssociatedItemsBuilder.define("fletcher",			"minecraft:bow");
		defaultAssociatedItemsBuilder.define("leatherworker",		"minecraft:leather");
		defaultAssociatedItemsBuilder.define("librarian",			"minecraft:bookshelf");
		defaultAssociatedItemsBuilder.define("mason",				"minecraft:brick");
		defaultAssociatedItemsBuilder.define("shepherd",			"minecraft:shears");
		defaultAssociatedItemsBuilder.define("toolsmith",			"minecraft:iron_pickaxe");
		defaultAssociatedItemsBuilder.define("weaponsmith",			"minecraft:iron_sword");
		defaultAssociatedItemsBuilder.define("wandering_trader",	"minecraft:emerald");
		defaultAssociatedItems = defaultAssociatedItemsBuilder.build();
	}

	public MerchantMarkersConfig(IcebergConfigSpec.Builder build)
	{
		build.comment("Client Configuration").push("client").push("visual_options");

		showThroughWalls = build.comment(" If markers should be visible through walls and other obstructions.").define("show_through_walls", true);
		showArrow = build.comment(" If markers should include an arrow under the profession-specific icon.").define("show_arrow", true);
		showOnMiniMap = build.comment(" If icons should show on minimaps. (Currently supports Xaero's Minimap).").define("show_on_minimap", true);
		overlayIndex = build.comment(" Which overlay graphic to use (0 = backpack, 1 = emerald, 2 = coin stack, 3 = bag, 4 = profession level, -1 = none).").defineInRange("overlay_icon", 3, -1, 4);
		opacity = build.comment(" The opacity of displayed markers and arrows.").defineInRange("opacity", 1.0, 0.1, 1.0);
		maxDistance = build.comment(" The maximum distance, in blocks, at which markers are visible.").defineInRange("max_distance", 64.0, 16.0, 256.0);
		fadePercent = build.comment(" The percent of the maximum distance at which markers will begin to fade out.").defineInRange("fade_percent", 25.0, 0.0, 100.0);
		iconScale = build.comment(" How large in-world markers should appear.").defineInRange("icon_scale", 1.0, 0.5, 2.0);
		minimapIconScale = build.comment(" How large markers should appear on minimaps.").defineInRange("minimap_icon_scale", 0.75, 0.5, 2.0);
		verticalOffset = build.comment(" How high above villagers markers should appear.  The default position (0) is right above name plates.").defineInRange("vertical_offset", 0, -128, 128);
		markerType = build.comment(" The types of markers to show above villagers.  Can be one of either \"items\", \"jobs\", \"generic\", or \"custom\".  These options mean:\n" +
								   "    \"items\" - Shows items from the associated item list below.\n" +
								   "    \"jobs\" - Shows the texture from the villager's job site block (like the brewing stand for clerics, and so on).\n" +
								   "    \"generic\" - Shows a generic icon that is the same for all villagers.\n" +
								   "    \"custom\" - Shows custom icons for each villager profession (these can be replaced with a resource pack).")
								   .defineInList("marker_type", "custom", Arrays.stream(MarkerType.values()).map(v -> v.name().toLowerCase()).toList());

		professionBlacklist = build.comment(" A list of professions to ignore when displaying markers. Use \"none\" for villagers with no profession.").define("profession_blacklist", Lists.newArrayList("none", "nitwit"));
		associatedItems = build.comment(" The items associated with each villager profession.  Only used when marker type is set to \"items\".").defineSubconfig("associated_items", defaultAssociatedItems, k -> Objects.nonNull(k), v -> Objects.nonNull(v) && v instanceof String str && ResourceLocation.isValidResourceLocation(str));

		build.pop().push("compatibility_options");

		enableOptifineWorkaround = build.comment(" If enabled, will force fast render on when using shaders with Optifine (due to a bug in Optifine, this is required for markers to render properly with some shaders).").define("optifine_workaround", true);

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
		Map<String, Object> configuredItems = associatedItems.get().valueMap();
		if (configuredItems.containsKey(profession) && 
			configuredItems.get(profession) instanceof String &&
			ResourceLocation.isValidResourceLocation((String)configuredItems.get(profession)))
		{
			return new ResourceLocation((String)configuredItems.get(profession));
		}

		if (defaultAssociatedItems.contains(profession))
		{
			return new ResourceLocation(defaultAssociatedItems.get(profession));
		}

		return null;
	}

	@Override
	protected void onLoad()
	{
		Markers.clearResourceCache();
		try
		{
			if (ModList.get().isLoaded("xaerominimap"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.XaeroMinimapHandler").getMethod("clearIconCache").invoke(null);
			}
			if (ModList.get().isLoaded("ftbchunks"))
			{
				Class.forName("com.anthonyhilyard.merchantmarkers.compat.FTBChunksHandler").getMethod("clearIconCache").invoke(null);
			}
		}
		catch (Exception e)
		{
			Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
		}
	}

	@Override
	protected <I extends IcebergConfig<?>> void setInstance(I instance)
	{
		INSTANCE = (MerchantMarkersConfig) instance;
	}

}