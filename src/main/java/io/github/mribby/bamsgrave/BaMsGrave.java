package io.github.mribby.bamsgrave;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = BaMsGrave.MOD_ID, name = BaMsGrave.MOD_NAME, version = BaMsGrave.MOD_VERSION, guiFactory = "io.github.mribby.bamsgrave.BaMsGuiFactory")
public class BaMsGrave {
    public static final String MOD_ID = "BaMsGRAVE";
    public static final String MOD_NAME = "BaM's Grave";
    public static final String MOD_VERSION = "@VERSION@";

    public static Logger logger;
    public static BaMsConfig config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();

        config = new BaMsConfig(event.getSuggestedConfigurationFile());
        config.sync(true);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MOD_ID.equals(event.modID)) {
            config.sync(false);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            boolean isSurvival = !player.capabilities.isCreativeMode;
            if (isSurvival || BaMsConfig.isCreativeEnabled) {
                GraveDigger digger = new GraveDigger(player);
                digger.dig();
            }
        }
    }
}
