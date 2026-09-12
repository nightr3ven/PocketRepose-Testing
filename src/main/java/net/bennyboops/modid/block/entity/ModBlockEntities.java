package net.bennyboops.modid.block.entity;

import net.bennyboops.modid.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<SuitcaseBlockEntity> SUITCASE_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath("pocket-repose", "suitcase"),
                    FabricBlockEntityTypeBuilder.create(
                            SuitcaseBlockEntity::new,
                            ModBlocks.SUITCASE,
                            ModBlocks.WHITE_SUITCASE,
                            ModBlocks.LIGHT_GRAY_SUITCASE,
                            ModBlocks.GRAY_SUITCASE,
                            ModBlocks.BLACK_SUITCASE,
                            ModBlocks.RED_SUITCASE,
                            ModBlocks.ORANGE_SUITCASE,
                            ModBlocks.YELLOW_SUITCASE,
                            ModBlocks.LIME_SUITCASE,
                            ModBlocks.GREEN_SUITCASE,
                            ModBlocks.CYAN_SUITCASE,
                            ModBlocks.LIGHT_BLUE_SUITCASE,
                            ModBlocks.BLUE_SUITCASE,
                            ModBlocks.PURPLE_SUITCASE,
                            ModBlocks.MAGENTA_SUITCASE,
                            ModBlocks.PINK_SUITCASE,
                            ModBlocks.SECRET_BARREL
                    ).build()
            );

    public static void registerBlockEntities() {
    }
}
