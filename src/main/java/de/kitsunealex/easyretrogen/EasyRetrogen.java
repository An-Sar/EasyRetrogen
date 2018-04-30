package de.kitsunealex.easyretrogen;

import de.kitsunealex.easyretrogen.command.CommandListRetrogens;
import de.kitsunealex.easyretrogen.init.ModConfig;
import de.kitsunealex.easyretrogen.retrogen.RetrogenHandler;
import de.kitsunealex.easyretrogen.retrogen.WorldTickHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import org.apache.logging.log4j.Logger;

import static de.kitsunealex.easyretrogen.util.Constants.*;

@Mod(modid = MODID, name = NAME, version = VERSION, useMetadata = true)
public class EasyRetrogen {

    @Mod.Instance(MODID)
    public static EasyRetrogen INSTANCE;
    public static Logger LOGGER;

    @Mod.EventHandler
    public void handlePreInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        ModConfig.loadConfig(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(RetrogenHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(WorldTickHandler.INSTANCE);
        RetrogenHandler.INSTANCE.onPreInit(event);
    }

    @Mod.EventHandler
    public void handleServerAboutToStart(FMLServerAboutToStartEvent event) {
        RetrogenHandler.INSTANCE.onServerAboutToStart(event);
    }

    @Mod.EventHandler
    public void handleServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandListRetrogens());
    }

    @Mod.EventHandler
    public void handleServerStopped(FMLServerStoppedEvent event) {
        RetrogenHandler.INSTANCE.onServerStopped(event);
    }

}
