package net.bennyboops.modid.block;

import net.bennyboops.modid.PocketRepose;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    public static final Block SUITCASE = registerBlock("suitcase",
            new SuitcaseBlock(settings("suitcase", Blocks.WOOL.brown())
                    .sound(SoundType.WOOL)
                    .strength(0.2f)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block WHITE_SUITCASE = registerBlock("white_suitcase",
            new SuitcaseBlock(settings("white_suitcase", Blocks.WOOL.white()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block BLACK_SUITCASE = registerBlock("black_suitcase",
            new SuitcaseBlock(settings("black_suitcase", Blocks.WOOL.black()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block LIGHT_GRAY_SUITCASE = registerBlock("light_gray_suitcase",
            new SuitcaseBlock(settings("light_gray_suitcase", Blocks.WOOL.lightGray()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block GRAY_SUITCASE = registerBlock("gray_suitcase",
            new SuitcaseBlock(settings("gray_suitcase", Blocks.WOOL.gray()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block RED_SUITCASE = registerBlock("red_suitcase",
            new SuitcaseBlock(settings("red_suitcase", Blocks.WOOL.red()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block ORANGE_SUITCASE = registerBlock("orange_suitcase",
            new SuitcaseBlock(settings("orange_suitcase", Blocks.WOOL.orange()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block YELLOW_SUITCASE = registerBlock("yellow_suitcase",
            new SuitcaseBlock(settings("yellow_suitcase", Blocks.WOOL.yellow()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block LIME_SUITCASE = registerBlock("lime_suitcase",
            new SuitcaseBlock(settings("lime_suitcase", Blocks.WOOL.lime()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block GREEN_SUITCASE = registerBlock("green_suitcase",
            new SuitcaseBlock(settings("green_suitcase", Blocks.WOOL.green()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block CYAN_SUITCASE = registerBlock("cyan_suitcase",
            new SuitcaseBlock(settings("cyan_suitcase", Blocks.WOOL.cyan()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block LIGHT_BLUE_SUITCASE = registerBlock("light_blue_suitcase",
            new SuitcaseBlock(settings("light_blue_suitcase", Blocks.WOOL.lightBlue()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block BLUE_SUITCASE = registerBlock("blue_suitcase",
            new SuitcaseBlock(settings("blue_suitcase", Blocks.WOOL.blue()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block PURPLE_SUITCASE = registerBlock("purple_suitcase",
            new SuitcaseBlock(settings("purple_suitcase", Blocks.WOOL.purple()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block MAGENTA_SUITCASE = registerBlock("magenta_suitcase",
            new SuitcaseBlock(settings("magenta_suitcase", Blocks.WOOL.magenta()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));
    public static final Block PINK_SUITCASE = registerBlock("pink_suitcase",
            new SuitcaseBlock(settings("pink_suitcase", Blocks.WOOL.pink()).sound(SoundType.WOOL).strength(0.2f).noOcclusion().lightLevel(state -> state.getValue(SuitcaseBlock.OPEN) ? 8 : 0)));

    public static final Block SECRET_BARREL = registerBlock("secret_barrel",
            new CustomSuitcaseBlock(
                    settings("secret_barrel", Blocks.BARREL)
                            .sound(SoundType.WOOD)
                            .strength(1.0f)
                            .noOcclusion(),
                    Block.box(0, 0, 0, 16, 15, 16),
                    SoundEvents.BARREL_OPEN,
                    SoundEvents.BARREL_CLOSE
            )
    );

    public static final Block PORTAL = registerBlock("portal",
            new PocketPortalBlock(settings("portal", Blocks.NETHER_PORTAL)
                    .sound(SoundType.LODESTONE)
                    .noOcclusion()
                    .lightLevel(state -> 10)
                    .strength(5.0f)));

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.tryBuild(PocketRepose.MOD_ID, name), block);

    }
    private static Item registerBlockItem(String name, Block block) {
        Identifier id = Identifier.fromNamespaceAndPath(PocketRepose.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }
    private static BlockBehaviour.Properties settings(String name, Block template) {
        ResourceKey<Block> key = ResourceKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(PocketRepose.MOD_ID, name)
        );
        return BlockBehaviour.Properties.ofFullCopy(template).setId(key);
    }
    public static void registerModBlocks() {
        PocketRepose.LOGGER.info("Registering mod blocks for" + PocketRepose.MOD_ID);
    }
}
