package net.bennyboops.modid.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CustomSuitcaseBlock extends SuitcaseBlock {
    protected final VoxelShape customShapeN;
    protected final VoxelShape customShapeS;
    protected final VoxelShape customShapeE;
    protected final VoxelShape customShapeW;

    protected final SoundEvent openSound;
    protected final SoundEvent closeSound;

    public CustomSuitcaseBlock(Properties settings,
                               VoxelShape shape,
                               SoundEvent openSound,
                               SoundEvent closeSound) {
        super(settings);
        this.customShapeN = shape;
        this.customShapeS = shape;
        this.customShapeE = shape;
        this.customShapeW = shape;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> customShapeN;
            case SOUTH -> customShapeS;
            case EAST -> customShapeE;
            case WEST -> customShapeW;
            default -> customShapeN;
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = super.useItemOn(stack, state, world, pos, player, hand, hit);
        playCustomSound(result, state, world, pos, player, stack);
        return result;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        InteractionResult result = super.useWithoutItem(state, world, pos, player, hit);
        playCustomSound(result, state, world, pos, player, ItemStack.EMPTY);
        return result;
    }

    private void playCustomSound(InteractionResult result, BlockState state, Level world, BlockPos pos,
                                 Player player, ItemStack stack) {
        if (result == InteractionResult.SUCCESS && !world.isClientSide() && (!player.isShiftKeyDown() || stack.isEmpty())) {
            boolean isOpen = state.getValue(OPEN);

            if (isOpen) {
                world.playSound(null, pos, closeSound,
                        SoundSource.BLOCKS, 0.3F, 1.0F);
            } else {
                world.playSound(null, pos, openSound,
                        SoundSource.BLOCKS, 0.3F, 1.0F);
            }
        }

    }
}
