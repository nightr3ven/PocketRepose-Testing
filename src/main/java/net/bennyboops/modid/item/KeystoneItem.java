package net.bennyboops.modid.item;

import net.bennyboops.modid.PocketRepose;
import net.bennyboops.modid.world.PortalChunkGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class KeystoneItem extends Item {

    private static final Identifier POCKET_DIMENSION_TYPE_ID = Identifier.fromNamespaceAndPath("pocket-repose", "pocket_dimension_type");
    private static final Identifier BOUND_MODEL_ID = Identifier.fromNamespaceAndPath("pocket-repose", "keystone");
    private static final Identifier UNBOUND_MODEL_ID = Identifier.fromNamespaceAndPath("pocket-repose", "keystone_unnamed");

    public KeystoneItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String keystoneName = stack.getHoverName().getString().toLowerCase();
        String defaultName = "item.pocket-repose.keystone";
        if (!stack.has(DataComponents.CUSTOM_NAME) || keystoneName.equals(defaultName)) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (isBound(stack)) {
            return InteractionResult.PASS;
        }
        String dimensionName = "pocket_dimension_" + keystoneName.replaceAll("[^a-z0-9_]", "");

        createOrLoadPersistentDimension(world.getServer(), dimensionName);

        // The old branch used hidden curse enchantments as a bound marker. The modern
        // component equivalent keeps the same glint without adding fake enchantments.
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.REPAIR_COST, 32767);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_CLUSTER_FALL, SoundSource.PLAYERS, 2.0F, 2.0F);
        return InteractionResult.SUCCESS;
    }


    private void createOrLoadPersistentDimension(MinecraftServer server, String dimensionName) {
        Identifier worldId = Identifier.fromNamespaceAndPath("pocket-repose", dimensionName);

        Path worldSavePath = server.getWorldPath(LevelResource.ROOT)
                .resolve("dimensions")
                .resolve("pocket-repose")
                .resolve(dimensionName);
        boolean dimensionExists = Files.exists(worldSavePath);

        ResourceKey<DimensionType> typeKey = ResourceKey.create(Registries.DIMENSION_TYPE, POCKET_DIMENSION_TYPE_ID);

        Registry<Biome> biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME);

        //RegistryKey<Biome> voidBiomeKey = RegistryKey.of(RegistryKeys.BIOME, new Identifier("minecraft", "the_void"));
        ResourceKey<Biome> voidBiomeKey = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("pocket-repose", "pocket_islands"));

        ChunkGenerator generator = new PortalChunkGenerator(biomeRegistry);

        long seed = server.overworld().getSeed();

        RuntimeLevelConfig config = new RuntimeLevelConfig()
                .setDimensionType(typeKey)
                .setGenerator(generator)
                .setSeed(seed);

        RuntimeLevelHandle handle = Fantasy.get(server)
                .getOrOpenPersistentLevel(worldId, config);

        registerDimension(server, dimensionName);

        if (!dimensionExists) {
            ServerLevel world = handle.asLevel();
            placeStructureImmediately(server, world, dimensionName);
            System.out.println("Created new dimension with structure: " + dimensionName);
        } else {
            System.out.println("Loaded existing dimension: " + dimensionName);
        }
    }

    private void placeStructureImmediately(MinecraftServer server, ServerLevel world, String dimensionName) {
        try {
            StructureTemplate template = server.getStructureManager()
                    .get(Identifier.fromNamespaceAndPath("pocket-repose", "pocket_island_01"))
                    .orElse(null);

            if (template != null) {
                BlockPos pos = new BlockPos(0, 64, 0);

                world.getChunk(pos);

                template.placeInWorld(
                        world,
                        pos,
                        pos,
                        new StructurePlaceSettings()
                                .setMirror(Mirror.NONE)
                                .setRotation(Rotation.NONE)
                                .setIgnoreEntities(false),
                        world.getRandom(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                );

                System.out.println("Immediately placed pocket island structure in new dimension: " + dimensionName);
            } else {
                System.err.println("Could not find structure template: pocket_island_01");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Path getStructureMarkerPath(MinecraftServer server, String dimensionName) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("pocket-repose")
                .resolve("pending_structures")
                .resolve(dimensionName + ".txt");
    }

    public static boolean isValidKeystone(ItemStack stack) {
        String keystoneName = stack.getHoverName().getString().toLowerCase();
        return stack.has(DataComponents.CUSTOM_NAME) &&
                !keystoneName.equals("item.pocket-repose.keystone") &&
                isBound(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (!stack.has(DataComponents.CUSTOM_NAME) ||
                stack.getHoverName().getString().toLowerCase().equals("item.pocket-repose.keystone")) {
            tooltip.accept(Component.literal("§7Rename to bind").withStyle(ChatFormatting.ITALIC));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!stack.has(DataComponents.CUSTOM_NAME) ||
                stack.getHoverName().getString().toLowerCase().equals("item.pocket-repose.keystone")) {
            stack.set(DataComponents.ITEM_MODEL, UNBOUND_MODEL_ID);
        } else {
            stack.set(DataComponents.ITEM_MODEL, BOUND_MODEL_ID);
        }

        if (isBound(stack) && stack.getOrDefault(DataComponents.REPAIR_COST, 0) < 32767) {
            stack.set(DataComponents.REPAIR_COST, 32767);
        }
    }

    public static boolean isBound(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    private void registerDimension(MinecraftServer server, String dimensionName) {
        Path registryDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("pocket-repose")
                .resolve("dimension_registry");

        try {
            Files.createDirectories(registryDir);
            Path registryFile = registryDir.resolve("registry.txt");

            // load existing lines
            Set<String> dims = new HashSet<>();
            if (Files.exists(registryFile)) {
                dims.addAll(Files.readAllLines(registryFile));
            }

            // add + save only if new
            if (dims.add(dimensionName)) {
                Files.write(registryFile, dims);
            }
        } catch (IOException e) {
            PocketRepose.LOGGER.error("Failed to write dimension registry", e);
        }
    }
}
