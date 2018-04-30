package de.kitsunealex.easyretrogen.retrogen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import de.kitsunealex.easyretrogen.EasyRetrogen;
import de.kitsunealex.easyretrogen.init.ModConfig;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;

public class WorldTickHandler {

    public static final WorldTickHandler INSTANCE = new WorldTickHandler();
    private int counter = 0;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if(event.world instanceof WorldServer) {
            WorldServer world = (WorldServer)event.world;

            if(event.phase == TickEvent.Phase.START) {
                counter = 0;
                RetrogenHandler.INSTANCE.getSemaphoreFor(world);
            }
            else {
                ListMultimap<ChunkPos, String> pending = RetrogenHandler.INSTANCE.getPendingWorkFor(world);

                if(pending == null) {
                    return;
                }

                ImmutableList<Map.Entry<ChunkPos, String>> forProcessing = ImmutableList.copyOf(Iterables.limit(pending.entries(), ModConfig.MAX_CHUNKS_PER_TICK + 1));

                for(Map.Entry<ChunkPos, String> entry : forProcessing) {
                    if(counter++ > ModConfig.MAX_CHUNKS_PER_TICK) {
                        EasyRetrogen.LOGGER.info("Completed {} retrogens this tick. There are {} left for world {}", counter, pending.size(), world.getWorldInfo().getWorldName());
                        return;
                    }

                    RetrogenHandler.INSTANCE.runRetrogen(entry.getKey(), world, entry.getValue());
                }
            }
        }
    }

}
