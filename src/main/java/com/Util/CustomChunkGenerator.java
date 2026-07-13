package com.Util;

import java.util.Random;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

public class CustomChunkGenerator extends ChunkGenerator {
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        return this.createChunkData(world);
    }
}
