package net.bennyboops.modid.item;

import net.bennyboops.modid.PocketRepose;
import net.bennyboops.modid.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class ModItemGroups {
    public static final CreativeModeTab POCKET_GROUP = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(PocketRepose.MOD_ID, "pocket"),
            FabricCreativeModeTab.builder().title(Component.translatable("itemgroup.pocket"))
                    .icon(() -> new ItemStack(ModItems.KEYSTONE)).displayItems((displayContext, entries) -> {

                        entries.accept(ModItems.KEYSTONE);

                        entries.accept(ModBlocks.SUITCASE);
                        entries.accept(ModBlocks.WHITE_SUITCASE);
                        entries.accept(ModBlocks.LIGHT_GRAY_SUITCASE);
                        entries.accept(ModBlocks.GRAY_SUITCASE);
                        entries.accept(ModBlocks.BLACK_SUITCASE);
                        entries.accept(ModBlocks.RED_SUITCASE);
                        entries.accept(ModBlocks.ORANGE_SUITCASE);
                        entries.accept(ModBlocks.YELLOW_SUITCASE);
                        entries.accept(ModBlocks.LIME_SUITCASE);
                        entries.accept(ModBlocks.GREEN_SUITCASE);
                        entries.accept(ModBlocks.CYAN_SUITCASE);
                        entries.accept(ModBlocks.LIGHT_BLUE_SUITCASE);
                        entries.accept(ModBlocks.BLUE_SUITCASE);
                        entries.accept(ModBlocks.MAGENTA_SUITCASE);
                        entries.accept(ModBlocks.PURPLE_SUITCASE);
                        entries.accept(ModBlocks.PINK_SUITCASE);

                        entries.accept(ModBlocks.SECRET_BARREL);

                        entries.accept(ModBlocks.PORTAL);

                        entries.accept(Blocks.ANVIL);

                    }).build());
    public static void registerItemGroups() {
        PocketRepose.LOGGER.info("Registering item groups for" + PocketRepose.MOD_ID);
    }
}
