package de.kitsunealex.easyretrogen.retrogen;

import com.google.common.collect.*;
import de.kitsunealex.easyretrogen.EasyRetrogen;
import de.kitsunealex.easyretrogen.init.ModConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

public class RetrogenHandler {

    public static final RetrogenHandler INSTANCE = new RetrogenHandler();
    private Map<String, TargetWorldWrapper> delegates = Maps.newHashMap();
    private Map<World, ListMultimap<ChunkPos, String>> pendingWork;
    private Map<World, ListMultimap<ChunkPos, String>> completedWork;
    private ConcurrentMap<World, Semaphore> completedWorkLocks;
    private Map<String, String> retros = Maps.newHashMap();

    public void onPreInit(FMLPreInitializationEvent event) {
        for(Marker marker : ModConfig.MARKERS) {
            for(String clazz : marker.getClasses()) {
                if(retros.put(clazz, marker.getMarker()) != null) {
                    EasyRetrogen.LOGGER.error("Duplicate class for multiple markers found: {}!", clazz);
                }
            }
        }
    }

    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        pendingWork = new MapMaker().weakKeys().makeMap();
        completedWork = new MapMaker().weakKeys().makeMap();
        completedWorkLocks = new MapMaker().weakKeys().makeMap();
        Set<IWorldGenerator> worldGens = ObfuscationReflectionHelper.getPrivateValue(GameRegistry.class, null, "worldGenerators");
        Map<IWorldGenerator, Integer> worldGenIndex = ObfuscationReflectionHelper.getPrivateValue(GameRegistry.class, null, "worldGeneratorIndex");
        ImmutableSet<String> immRetros = ImmutableSet.copyOf(retros.keySet());

        for(String clazz : immRetros) {
            if(!delegates.containsKey(clazz)) {
                EasyRetrogen.LOGGER.info("Substituting worldgenerator {} with delegate", clazz);

                for(Iterator<IWorldGenerator> iterator = worldGens.iterator(); iterator.hasNext();) {
                    IWorldGenerator generator = iterator.next();

                    if(generator.getClass().getName().equals(clazz)) {
                        iterator.remove();
                        TargetWorldWrapper wrapper = new TargetWorldWrapper(generator, clazz);
                        worldGens.add(wrapper);
                        Integer index = worldGenIndex.remove(generator);
                        worldGenIndex.put(wrapper, index);
                        EasyRetrogen.LOGGER.info("Successfully substituted {} with delegate", clazz);
                        delegates.put(clazz, wrapper);
                        break;
                    }
                }

                if(!delegates.containsKey(clazz)) {
                    EasyRetrogen.LOGGER.warn("Easy Retrogen was not able to locate world generator class {}, skipping...", clazz);
                    retros.remove(clazz);
                }
            }
        }
    }

    public void onServerStopped(FMLServerStoppedEvent event) {
        Set<IWorldGenerator> worldGens = ObfuscationReflectionHelper.getPrivateValue(GameRegistry.class, null, "worldGenerators");
        Map<IWorldGenerator, Integer> worldGenIndex = ObfuscationReflectionHelper.getPrivateValue(GameRegistry.class, null, "worldGeneratorIndex");

        for(TargetWorldWrapper wrapper : delegates.values()) {
            worldGens.remove(wrapper);
            Integer index = worldGenIndex.remove(wrapper);
            worldGens.add(wrapper.getDelegate());
            worldGenIndex.put(wrapper.getDelegate(), index);
        }

        delegates.clear();
    }

    @SubscribeEvent
    public void onChunkLoaded(ChunkDataEvent.Load event) {
        if(event.getWorld() instanceof WorldServer) {
            WorldServer world = (WorldServer)event.getWorld();
            getSemaphoreFor(world);
            Chunk chunk = event.getChunk();
            Set<String> existingGens = Sets.newHashSet();
            NBTTagCompound compound = new NBTTagCompound();

            for(Marker marker : ModConfig.MARKERS) {
                NBTTagCompound markerTag = new NBTTagCompound();
                NBTTagList tagList = markerTag.getTagList("list", 8);

                for(int i = 0; i < tagList.tagCount(); i++) {
                    existingGens.add(tagList.getStringTagAt(i));
                }

                Sets.SetView<String> differences = Sets.difference(marker.getClasses(), existingGens);

                for(String clazz : differences) {
                    if(retros.containsKey(clazz)) {
                        queueRetrogen(chunk.getPos(), world, clazz);
                    }
                }
            }

            for(String clazz : existingGens) {
                completeRetrogen(chunk.getPos(), world, clazz);
            }
        }
    }

    @SubscribeEvent
    public void onChunkSaved(ChunkDataEvent.Save event) {
        if(event.getWorld() instanceof WorldServer) {
            WorldServer world = (WorldServer)event.getWorld();
            getSemaphoreFor(world).acquireUninterruptibly();

            try {
                if(completedWork.containsKey(world)) {
                    ListMultimap<ChunkPos, String> doneChunks = completedWork.get(world);
                    List<String> classes = doneChunks.get(event.getChunk().getPos());

                    if(classes.isEmpty()) {
                        return;
                    }

                    NBTTagCompound compound = event.getData();

                    for(String clazz : classes) {
                        String marker = retros.get(clazz);

                        if(marker == null) {
                            EasyRetrogen.LOGGER.info("Encountered retrogen class {} with no existing marker, removing from chunk.");
                            continue;
                        }

                        NBTTagList tagList;

                        if(compound.hasKey("marker")) {
                            tagList = compound.getCompoundTag(marker).getTagList("list", 8);
                        }
                        else {
                            NBTTagCompound retroCompound = new NBTTagCompound();
                            tagList = new NBTTagList();
                            retroCompound.setTag("list", tagList);
                            compound.setTag(marker, retroCompound);
                        }

                        tagList.appendTag(new NBTTagString(clazz));
                    }
                }
            }
            finally {
                getSemaphoreFor(world).release();
            }
        }
    }

    public void queueRetrogen(ChunkPos pos, WorldServer world, String clazz) {
        ListMultimap<ChunkPos, String> currentWork = pendingWork.get(world);

        if(currentWork == null) {
            currentWork = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);
            pendingWork.put(world, currentWork);
        }

        currentWork.put(pos, clazz);
    }

    public void runRetrogen(ChunkPos pos, WorldServer world, String clazz) {
        long seed = world.getSeed();
        Random random = new Random(seed);
        long xSeed = random.nextLong() >> 2 + 1L;
        long zSeed = random.nextLong() >> 2 + 1L;
        long chunkSeed = (xSeed * pos.x + zSeed * pos.z) ^ seed;
        random.setSeed(chunkSeed);
        ChunkProviderServer chunkProvider = world.getChunkProvider();
        IChunkGenerator generator = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderServer.class, chunkProvider, "chunkGenerator", "field_186029_c");
        delegates.get(clazz).getDelegate().generate(random, pos.x, pos.z, world, generator, chunkProvider);
        completeRetrogen(pos, world, clazz);
    }

    public void completeRetrogen(ChunkPos pos, World world, String clazz) {
        ListMultimap<ChunkPos, String> pendingMap = pendingWork.get(world);

        if(pendingMap != null && pendingMap.containsKey(pos)) {
            pendingMap.remove(pos, clazz);
        }

        getSemaphoreFor(world).acquireUninterruptibly();

        try {
            ListMultimap<ChunkPos, String> completedMap = completedWork.get(world);

            if(completedMap == null) {
                completedMap = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);
                completedWork.put(world, completedMap);
            }

            completedMap.put(pos, clazz);
        }
        finally {
            getSemaphoreFor(world).release();
        }
    }

    public Semaphore getSemaphoreFor(World world) {
        completedWorkLocks.putIfAbsent(world, new Semaphore(1));
        return completedWorkLocks.get(world);
    }

    public ListMultimap<ChunkPos, String> getPendingWorkFor(World world) {
        return pendingWork.get(world);
    }

    public ListMultimap<ChunkPos, String> getCompletedWorkFor(World world) {
        return completedWork.get(world);
    }

}
