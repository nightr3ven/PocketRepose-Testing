package net.bennyboops.modid.block;

import com.mojang.serialization.MapCodec;
import net.bennyboops.modid.PocketRepose;
import net.bennyboops.modid.block.entity.ModBlockEntities;
import net.bennyboops.modid.block.entity.SuitcaseBlockEntity;
import net.bennyboops.modid.data.PlayerEntryData;
import net.bennyboops.modid.item.KeystoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuitcaseBlock extends BaseEntityBlock {
    public static final MapCodec<SuitcaseBlock> CODEC = simpleCodec(SuitcaseBlock::new);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    private final static VoxelShape SHAPE_N = Block.box(0, 0, 2, 16, 4, 14);
    private final static VoxelShape SHAPE_S = Block.box(0, 0, 2, 16, 4, 14);
    private final static VoxelShape SHAPE_E = Block.box(2, 0, 0, 14, 4, 16);
    private final static VoxelShape SHAPE_W = Block.box(2, 0, 0, 14, 4, 16);

    public SuitcaseBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState()
                .setValue(OPEN, false)
                .setValue(FACING, Direction.NORTH)
                .setValue(COLOR, DyeColor.BROWN));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (!world.isClientSide()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SuitcaseBlockEntity suitcase) {
                String keystoneName = suitcase.getBoundKeystoneName();
                if (keystoneName != null) {
                    List<SuitcaseBlockEntity.EnteredPlayerData> players = suitcase.getEnteredPlayers();
                    for (SuitcaseBlockEntity.EnteredPlayerData player : players) {
                        suitcase.updatePlayerSuitcasePosition(player.uuid, pos);
                        Map<String, BlockPos> suitcases = SuitcaseBlockEntity.SUITCASE_REGISTRY.computeIfAbsent(
                                keystoneName, k -> new HashMap<>()
                        );
                        suitcases.put(player.uuid, pos);
                    }
                }
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level world, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, world, pos, player, hand, heldItem);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return interact(state, world, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private InteractionResult interact(BlockState state, Level world, BlockPos pos, Player player,
                                       InteractionHand hand, ItemStack heldItem) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof SuitcaseBlockEntity suitcase)) {
            return InteractionResult.PASS;
        }
        if (!suitcase.canOpenInDimension(world)) {
            world.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.BLOCKS, 0.3F, 2.0F);
            player.sendOverlayMessage(Component.literal("§c☒"));
            return InteractionResult.SUCCESS;
        }
        String boundKeystone = suitcase.getBoundKeystoneName();

        // Handle keystone interactions
        if (heldItem.getItem() instanceof KeystoneItem) {
            String keystoneName = heldItem.getHoverName().getString().toLowerCase().replaceAll("[^a-z0-9_]", "");
            // Handle locking with keystone
            if (boundKeystone != null && boundKeystone.equals(keystoneName)) {
                boolean newLockState = !suitcase.isLocked();
                suitcase.setLocked(newLockState);
                world.playSound(null, pos,
                        newLockState ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_DOOR_OPEN,
                        SoundSource.BLOCKS, 0.3F, 2.0F);
                player.sendOverlayMessage(Component.literal(newLockState ? "§7☒" : "§7☐"));
                return InteractionResult.SUCCESS;
            }
            // Prevent binding if suitcase is locked
            if (suitcase.isLocked()) {
                world.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE,
                        SoundSource.BLOCKS, 0.3F, 2.0F);
                player.sendOverlayMessage(Component.literal("§c☒"));
                return InteractionResult.FAIL;
            }
            // Binding logic
            if (keystoneName.equals("item.pocket-repose.keystone")) {
                player.sendSystemMessage(Component.literal("§cName the key to bind."));
                return InteractionResult.FAIL;
            }
            if (!KeystoneItem.isValidKeystone(heldItem)) {
                return InteractionResult.FAIL;
            }
            suitcase.bindKeystone(keystoneName);
            world.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK,
                    SoundSource.BLOCKS, 2.0F, 0.0F);
            return InteractionResult.SUCCESS;
        }
        // Handle opening/closing
        if (!player.isShiftKeyDown() || heldItem.isEmpty()) {
            if (boundKeystone == null) {
                world.playSound(null, pos, SoundEvents.CHAIN_PLACE,
                        SoundSource.BLOCKS, 0.5F, 2.0F);
                return InteractionResult.FAIL;
            }
            if (suitcase.isLocked()) {
                world.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE,
                        SoundSource.BLOCKS, 0.3F, 2.0F);
                return InteractionResult.FAIL;
            }
            boolean isOpen = state.getValue(OPEN);
            world.setBlockAndUpdate(pos, state.setValue(OPEN, !isOpen));
            if (!isOpen) {
                world.playSound(null, pos, SoundEvents.LADDER_STEP,
                        SoundSource.BLOCKS, 0.3F, 0.0F);
                world.playSound(null, pos, SoundEvents.CHEST_LOCKED,
                        SoundSource.BLOCKS, 0.3F, 2.0F);
            } else {
                world.playSound(null, pos, SoundEvents.LADDER_BREAK,
                        SoundSource.BLOCKS, 0.3F, 0.0F);
                world.playSound(null, pos, SoundEvents.BAMBOO_WOOD_TRAPDOOR_CLOSE,
                        SoundSource.BLOCKS, 0.3F, 0.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!world.isClientSide() && entity instanceof ServerPlayer player) {
            // Only teleport if suitcase is open and player is sneaking
            if (!state.getValue(OPEN) || !player.isShiftKeyDown()) {
                return;
            }
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof SuitcaseBlockEntity suitcase)) {
                return;
            }
            String keystoneName = suitcase.getBoundKeystoneName();
            if (keystoneName == null) {
                return;
            }
            String dimensionName = "pocket_dimension_" + keystoneName;
            Identifier dimensionId = Identifier.fromNamespaceAndPath("pocket-repose", dimensionName);
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            ServerLevel targetWorld = world.getServer().getLevel(dimensionKey);
            if (targetWorld != null) {
                boolean wasFirstTime = suitcase.isFirstTimeEntering(player);
                suitcase.playerEntered(player);
                if (wasFirstTime) {
                    PocketRepose.ENTER_POCKET_DIMENSION.trigger(player);
                }

                player.stopRiding();
                player.hurtMarked = true;
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0f;

                PlayerEntryData ped = PlayerEntryData.get(targetWorld);
                Vec3 dest = ped.getEntryPos();

                float yaw = ped.getEntryYaw();
                float pitch = player.getXRot();

                TeleportTransition transition = new TeleportTransition(
                        targetWorld, dest, Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING
                );
                player.teleport(transition);

                player.connection.send(new ClientboundStopSoundPacket(null, null));

                world.playSound(
                        null,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        SoundEvents.BUNDLE_DROP_CONTENTS,
                        SoundSource.PLAYERS,
                        2.0f, 1.0f
                );
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_N;
            case SOUTH -> SHAPE_S;
            case EAST -> SHAPE_E;
            case WEST -> SHAPE_W;
            default -> SHAPE_N;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0f;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
        builder.add(FACING);
        builder.add(COLOR);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuitcaseBlockEntity(pos, state);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(world, pos, state, includeData);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SuitcaseBlockEntity suitcase) {
            applySuitcaseData(stack, suitcase, world);
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack stack = new ItemStack(this);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof SuitcaseBlockEntity suitcase) {
            applySuitcaseData(stack, suitcase, builder.getLevel());
        }
        return List.of(stack);
    }

    private static void applySuitcaseData(ItemStack stack, SuitcaseBlockEntity suitcase, LevelReader world) {
        String boundKeystone = suitcase.getBoundKeystoneName();
        if (boundKeystone == null) {
            return;
        }

        stack.set(DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(ModBlockEntities.SUITCASE_BLOCK_ENTITY,
                        suitcase.saveCustomOnly(world.registryAccess())));

        List<Component> lore = new ArrayList<>();
        if (!suitcase.getEnteredPlayers().isEmpty()) {
            lore.add(Component.literal("§c⚠ Contains " + suitcase.getEnteredPlayers().size() + " traveler(s)!")
                    .withStyle(ChatFormatting.RED));
        }

        String displayName = boundKeystone.replace("_", " ");
        lore.add(Component.literal(suitcase.isLocked()
                        ? "Bound to: §k" + displayName
                        : "Bound to: " + displayName)
                .withStyle(ChatFormatting.GRAY));
        lore.add(Component.literal(suitcase.isLocked() ? "§cLocked" : "§aUnlocked")
                .withStyle(ChatFormatting.GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int type, int data) {
        super.triggerEvent(state, world, pos, type, data);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity != null && blockEntity.triggerEvent(type, data);
    }
}
