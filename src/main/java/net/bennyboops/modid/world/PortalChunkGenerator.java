package net.bennyboops.modid.world;

import net.bennyboops.modid.block.ModBlocks;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class PortalChunkGenerator extends VoidChunkGenerator {
    private final BlockState portalState = ModBlocks.PORTAL.defaultBlockState();

    public PortalChunkGenerator(Registry<Biome> biomeRegistry) {
        super(biomeRegistry,
                ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("pocket-repose", "pocket_islands")));
    }

    @Override
    public void applyBiomeDecoration(
            WorldGenLevel world,
            ChunkAccess chunk,
            StructureManager structureAccessor
    ) {
        super.applyBiomeDecoration(world, chunk, structureAccessor);

        ChunkPos chunkPos = chunk.getPos();
        for (int dy = -64; dy <= -61; dy++) {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int worldX = (chunkPos.x() << 4) + dx;
                    int worldZ = (chunkPos.z() << 4) + dz;
                    BlockPos blockPos = new BlockPos(worldX, dy, worldZ);
                    world.setBlock(blockPos, portalState, Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
