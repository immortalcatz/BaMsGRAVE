package io.github.mribby.bamsgrave;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = "BaMsGRAVE", name = "BaM's Grave", version = "@VERSION@")
public class BaMsGrave {
    public static boolean storeXP = true;
    public static boolean isCreativeEnabled = false;
    public static boolean needChestToMakeGrave = true;
    public static boolean needSignToMakeSign = true;
    private static String[] blacklist = {
            "minecraft:bedrock",
            "CL: net.minecraft.block.BlockContainer"
    };
    private static String[] conversions = {
            "*:oreGold minecraft:stone",
            "*:oreIron minecraft:stone",
            "*:oreLapis minecraft:stone",
            "*:oreDiamond minecraft:stone",
            "*:oreRedstone minecraft:stone",
            "*:oreEmerald minecraft:stone",
            "*:oreCoal minecraft:stone",
            "*:oreQuartz minecraft:netherrack"
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        storeXP = config.get("general", "StoreXP", storeXP, "Should graves store the player's experience?").getBoolean();
        isCreativeEnabled = config.get("general", "GravesForCreativeMode", isCreativeEnabled, "Do players in creative mode make graves?").getBoolean();
        needChestToMakeGrave = config.get("general", "NeedChestToMakeGrave", needChestToMakeGrave, "Does a player need one or more chests to make a grave?").getBoolean();
        needSignToMakeSign = config.get("general", "NeedSignToMakeSign", needSignToMakeSign, "Does a player need a sign in his inventory to have a sign on his grave?").getBoolean();
        blacklist = config.get("general", "BlocksBlacklist", blacklist, "Use this to protect valuable blocks").getStringList();
        conversions = config.get("general", "TombstoneConversions", conversions, "Use this to duplication").getStringList();
        config.save();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            boolean isSurvival = !player.capabilities.isCreativeMode;
            if (isSurvival || isCreativeEnabled) {
                GraveDigger digger = new GraveDigger(player);
                digger.dig();
            }
        }
    }
}
