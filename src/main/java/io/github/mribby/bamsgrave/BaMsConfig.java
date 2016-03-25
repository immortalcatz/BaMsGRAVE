package io.github.mribby.bamsgrave;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class BaMsConfig extends Configuration {
    public static final String CATEGORY_TOMBSTONE = "tombstone";
    public static final String CATEGORY_COFFIN = "coffin";

    public static final String ORE_DICT_ID = "**";
    public static final String ORE_DICT_PREFIX = ORE_DICT_ID + ":";
    public static final String CLASS_ID = "JAVA";
    public static final String CLASS_PREFIX = CLASS_ID + ":";

    private static class Defaults {
        // GENERAL
        private static final int MINIMUM_HEIGHT = 5;
        private static final boolean IS_CREATIVE_ENABLED = false;
        private static final boolean USE_WHITELIST = true;
        private static final String[] WHITELIST = {
                "minecraft:air",
                "minecraft:stone",
                "minecraft:grass",
                "minecraft:dirt",
                "minecraft:sapling",
                "minecraft:flowing_water",
                "minecraft:water",
                "minecraft:flowing_lava",
                "minecraft:lava",
                "minecraft:sand",
                "minecraft:gravel",
                "minecraft:log",
                "minecraft:log2",
                "minecraft:leaves",
                "minecraft:sandstone",
                "minecraft:web",
                "minecraft:tallgrass",
                "minecraft:deadbush",
                "minecraft:yellow_flower",
                "minecraft:red_flower",
                "minecraft:brown_mushroom",
                "minecraft:red_mushroom",
                "minecraft:torch",
                "minecraft:fire",
                "minecraft:farmland",
                "minecraft:standing_sign",
                "minecraft:wall_sign",
                "minecraft:snow_layer",
                "minecraft:ice",
                "minecraft:snow",
                "minecraft:cactus",
                "minecraft:clay",
                "minecraft:reeds",
                "minecraft:pumpkin",
                "minecraft:netherrack",
                "minecraft:soul_sand",
                "minecraft:brown_mushroom_block",
                "minecraft:red_mushroom_block",
                "minecraft:melon_block",
                "minecraft:pumpkin_stem",
                "minecraft:melon_stem",
                "minecraft:vine",
                "minecraft:mycelium",
                "minecraft:waterlily",
                "minecraft:nether_wart",
                "minecraft:end_stone",
                "minecraft:cocoa",
                "minecraft:flower_pot",
                "minecraft:carrots",
                "minecraft:potatoes"
        };
        private static final String[] BLACKLIST = {
                ORE_DICT_PREFIX + "oreGold",
                ORE_DICT_PREFIX + "oreDiamond",
                ORE_DICT_PREFIX + "oreEmerald",
                CLASS_PREFIX + "net/minecraft/block/BlockContainer"
        };

        // TOMBSTONE
        private static final boolean NEED_SIGN_TO_MAKE_SIGN = true;
        private static final String DATE_PATTERN = "yyyy-MM-dd";
        private static final String TIME_PATTERN = "HH:mm:ss";
        private static final String FLOWER_POT_NAME = "minecraft:flower_pot 0 {Item:\"minecraft:deadbush\"}";
        private static final String TOMBSTONE_NAME = "minecraft:quartz_block 1";
        private static final String[] TOMBSTONES = {
                "minecraft:quartz_ore;minecraft:netherrack",
                ORE_DICT_PREFIX + "oreGold;minecraft:stone",
                ORE_DICT_PREFIX + "oreIron;minecraft:stone",
                ORE_DICT_PREFIX + "oreLapis;minecraft:stone",
                ORE_DICT_PREFIX + "oreDiamond;minecraft:stone",
                ORE_DICT_PREFIX + "oreRedstone;minecraft:stone",
                ORE_DICT_PREFIX + "oreEmerald;minecraft:stone",
                ORE_DICT_PREFIX + "oreCoal;minecraft:stone"
        };

        // COFFIN
        private static final boolean NEED_CHEST_TO_MAKE_COFFIN = true;
        private static final boolean STORE_XP = true;
    }

    // General
    public static int minimumHeight;
    public static boolean isCreativeEnabled;
    private static boolean useWhitelist;
    private static List<BlockMatcher> whitelistMatchers;
    private static List<BlockMatcher> blacklistMatchers;

    // Tombstone
    public static boolean needSignToMakeSign;
    public static DateFormat dateFormat;
    public static DateFormat timeFormat;
    private static BlockSetter flowerPotSetter;
    private static BlockSetter tombstoneSetter;
    private static Map<BlockMatcher, BlockSetter> groundToTombstone;

    // Coffin
    public static boolean needChestToMakeCoffin;
    public static boolean storeXP;

    private List<String> propOrder = new ArrayList<String>();

    public BaMsConfig(File file) {
        super(file);
    }

    public void sync(boolean load) {
        if (load) {
            load();
        }

        minimumHeight = get(CATEGORY_GENERAL, "MinimumHeight", Defaults.MINIMUM_HEIGHT, "The minimum height to place graves").getInt();
        isCreativeEnabled = get(CATEGORY_GENERAL, "GravesForCreativeMode", Defaults.IS_CREATIVE_ENABLED, "Do players in creative mode make graves?").getBoolean();
        useWhitelist = get(CATEGORY_GENERAL, "UseBlocksWhitelist", Defaults.USE_WHITELIST, "Should grave placement check the whitelist instead of the blacklist?").getBoolean();
        String[] whitelist = get(CATEGORY_GENERAL, "BlocksWhitelist", Defaults.WHITELIST, "These blocks are not protected during grave placement.").getStringList();
        String[] blacklist = get(CATEGORY_GENERAL, "BlocksBlacklist", Defaults.BLACKLIST, "These blocks are protected during grave placement.").getStringList();
        setCategoryPropertyOrder(CATEGORY_GENERAL);

        needSignToMakeSign = get(CATEGORY_TOMBSTONE, "NeedSignToMakeSign", Defaults.NEED_SIGN_TO_MAKE_SIGN, "Does the player need a sign in his inventory to have a sign on his grave?").getBoolean();
        String datePattern = get(CATEGORY_TOMBSTONE, "DateFormat", Defaults.DATE_PATTERN, "The date format used when engraving the tombstone").getString();
        String timePattern = get(CATEGORY_TOMBSTONE, "TimeFormat", Defaults.TIME_PATTERN, "The time format used when engraving the tombstone").getString();
        String flowerPotName = get(CATEGORY_TOMBSTONE, "FlowerPot", Defaults.FLOWER_POT_NAME, "The flower pot placed on top of the tombstone").getString();
        String tombstoneName = get(CATEGORY_TOMBSTONE, "DefaultTombstone", Defaults.TOMBSTONE_NAME, "Default tombstone used when not defined in Tombstones").getString();
        String[] tombstones = get(CATEGORY_TOMBSTONE, "Tombstones", Defaults.TOMBSTONES, "Syntax: <GroundBlock>;<TombstoneBlock>").getStringList();
        setCategoryPropertyOrder(CATEGORY_TOMBSTONE);

        needChestToMakeCoffin = get(CATEGORY_COFFIN, "NeedChestToMakeCoffin", Defaults.NEED_CHEST_TO_MAKE_COFFIN, "Does the player need one or more chests to make a coffin?").getBoolean();
        storeXP = get(CATEGORY_COFFIN, "StoreXP", Defaults.STORE_XP, "Should graves store the player's experience?").getBoolean();
        setCategoryPropertyOrder(CATEGORY_COFFIN);

        dateFormat = new SimpleDateFormat(datePattern);
        timeFormat = new SimpleDateFormat(timePattern);
        flowerPotSetter = BlockSetter.parse(flowerPotName);
        tombstoneSetter = BlockSetter.parse(tombstoneName);

        if (useWhitelist) {
            whitelistMatchers = new ArrayList<BlockMatcher>();
            for (String line : whitelist) {
                try {
                    BlockMatcher matcher = BlockMatcher.parse(line);
                    if (matcher != null) {
                        blacklistMatchers.add(matcher);
                    }
                } catch (Exception e) {
                    BaMsGrave.logger.error("Could not add '%s' to whitelist", line);
                }
            }
        } else {
            blacklistMatchers = new ArrayList<BlockMatcher>();
            for (String line : blacklist) {
                try {
                    BlockMatcher matcher = BlockMatcher.parse(line);
                    if (matcher != null) {
                        blacklistMatchers.add(matcher);
                    }
                } catch (Exception e) {
                    BaMsGrave.logger.error("Could not add '%s' to blacklist", line);
                }
            }
        }

        groundToTombstone = new HashMap<BlockMatcher, BlockSetter>();
        Splitter splitter = Splitter.on(';').omitEmptyStrings().trimResults();
        for (String line : tombstones) {
            try {
                String[] parts = Iterables.toArray(splitter.split(line), String.class);
                BlockSetter setter = BlockSetter.parse(parts[1]);
                if (setter == null) continue;
                BlockMatcher matcher = BlockMatcher.parse(parts[0]);
                if (matcher != null) {
                    groundToTombstone.put(matcher, setter);
                }
            } catch (Exception e) {
                BaMsGrave.logger.error("Could not add conversion: %s", line);
            }
        }

        if (hasChanged()) {
            save();
        }
    }

    private Configuration setCategoryPropertyOrder(String category) {
        Configuration config = setCategoryPropertyOrder(category, propOrder);
        propOrder = new ArrayList<String>();
        return config;
    }

    private Property get(Property prop) {
        propOrder.add(prop.getName());
        prop.setLanguageKey("bamsgrave.config." + prop.getName());
        return prop;
    }

    @Override
    public Property get(String category, String key, String defaultValue, String comment, Property.Type type) {
        return get(super.get(category, key, defaultValue, comment, type));
    }

    @Override
    public Property get(String category, String key, String[] defaultValues, String comment, Property.Type type) {
        return get(super.get(category, key, defaultValues, comment, type));
    }

    public static boolean isWhitelisted(IBlockState state) {
        if (useWhitelist) {
            for (BlockMatcher matcher : whitelistMatchers) {
                if (matcher.matches(state)) {
                    return true;
                }
            }
            return false;
        } else {
            return !isBlacklisted(state);
        }
    }

    public static boolean isBlacklisted(IBlockState state) {
        if (useWhitelist) {
            return !isWhitelisted(state);
        } else {
            for (BlockMatcher matcher : blacklistMatchers) {
                if (matcher.matches(state)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static boolean setTombstoneWall(World world, BlockPos pos, IBlockState ground, int flags) {
        if (isBlacklisted(world.getBlockState(pos))) {
            return false;
        }
        for (Map.Entry<BlockMatcher, BlockSetter> entry : groundToTombstone.entrySet()) {
            if (entry.getKey().matches(ground)) {
                return entry.getValue().setBlock(world, pos, flags);
            }
        }
        return tombstoneSetter != null && tombstoneSetter.setBlock(world, pos, flags);
    }

    public static boolean setFlowerPot(World world, BlockPos pos, int flags) {
        return world.isAirBlock(pos) && flowerPotSetter != null && flowerPotSetter.setBlock(world, pos, flags);
    }

    public static boolean setCoffin(World world, BlockPos pos, IBlockState state, int flags) {
        return state.getBlock().canPlaceBlockAt(world, pos) && setBlockState(world, pos, state, flags);
    }

    public static boolean setBlockState(World world, BlockPos pos, IBlockState state, int flags) {
        return !isBlacklisted(world.getBlockState(pos)) && world.setBlockState(pos, state, flags);
    }

    public static String getFormattedDate(Date date) {
        return dateFormat.format(date);
    }

    public static String getFormattedTime(Date date) {
        return timeFormat.format(date);
    }
}
