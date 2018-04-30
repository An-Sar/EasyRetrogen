package de.kitsunealex.easyretrogen.retrogen;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

public class TargetWorldWrapper implements IWorldGenerator {

    private IWorldGenerator delegate;
    private String clazz;

    public TargetWorldWrapper(IWorldGenerator delegate, String clazz) {
        this.delegate = delegate;
        this.clazz = clazz;
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        delegate.generate(random, chunkX, chunkZ, world, chunkGenerator, chunkProvider);
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        RetrogenHandler.INSTANCE.completeRetrogen(pos, world, clazz);
    }

    public IWorldGenerator getDelegate() {
        return delegate;
    }

    public String getClazz() {
        return clazz;
    }

}
