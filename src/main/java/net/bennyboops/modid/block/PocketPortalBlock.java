package net.bennyboops.modid.block;

import net.bennyboops.modid.block.entity.SuitcaseBlockEntity;
import net.bennyboops.modid.block.entity.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PocketPortalBlock extends Block {
    private static final Map<String, PlayerPositionData> LAST_KNOWN_POSITIONS = new HashMap<>();
    private static final int SEARCH_RADIUS_CHUNKS = 12;
    public static class PlayerPositionData {
        public final double x;
        public final double y;
        public final double z;
        public final float yaw;
        public final float pitch;
        public final long timestamp;
        public PlayerPositionData(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public PocketPortalBlock(Properties settings) {
        super(settings);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }


    public static void storePlayerPosition(ServerPlayer player) {
        LAST_KNOWN_POSITIONS.put(
                player.getStringUUID(),
                new PlayerPositionData(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot())
        );
    }

    private boolean attemptPlayerInventorySuitcaseTeleport(Level world, ServerLevel overworld, ServerPlayer player, String keystoneName) {
        for (ServerPlayer serverPlayer : world.getServer().getPlayerList().getPlayers()) {
            if (scanPlayerInventoryForSuitcase(serverPlayer, player, keystoneName, world, overworld)) {
                return true;
            }
        }
        return false;
    }

    private boolean scanPlayerInventoryForSuitcase(ServerPlayer inventoryOwner, ServerPlayer exitingPlayer,
                                                   String keystoneName, Level world, ServerLevel overworld) {
        for (int i = 0; i < inventoryOwner.getInventory().getContainerSize(); i++) {
            ItemStack stack = inventoryOwner.getInventory().getItem(i);
            if (isSuitcaseItemWithKeystone(stack, keystoneName)) {
                cleanUpSuitcaseItemNbt(stack, exitingPlayer, keystoneName);
                inventoryOwner.getInventory().setItem(i, stack);
                teleportToPosition(world, exitingPlayer, overworld,
                        inventoryOwner.getX(), inventoryOwner.getY() + 1.0, inventoryOwner.getZ(),
                        exitingPlayer.getYRot(), exitingPlayer.getXRot());

                return true;
            }
        }
        return false;
    }

    private boolean isSuitcaseItemWithKeystone(ItemStack stack, String keystoneName) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem) ||
                !(((BlockItem) stack.getItem()).getBlock() instanceof SuitcaseBlock)) {
            return false;
        }

        CompoundTag beTag = getSuitcaseData(stack);
        if (beTag == null) return false;

        return keystoneName.equals(beTag.getStringOr("BoundKeystone", ""));
    }

    private void cleanUpSuitcaseItemNbt(ItemStack stack, ServerPlayer player, String keystoneName) {
        CompoundTag beTag = getSuitcaseData(stack);
        if (beTag == null) return;
        ListTag playersList = beTag.getList("EnteredPlayers").orElse(null);
        if (playersList != null) {
            ListTag newPlayersList = new ListTag();
            boolean playerFound = false;
            for (int i = 0; i < playersList.size(); i++) {
                CompoundTag playerData = playersList.getCompoundOrEmpty(i);
                if (!player.getStringUUID().equals(playerData.getStringOr("UUID", ""))) {
                    newPlayersList.add(playerData);
                } else {
                    playerFound = true;
                }
            }
            if (playerFound) {
                beTag.put("EnteredPlayers", newPlayersList);
                setSuitcaseData(stack, beTag);
                int remainingPlayers = newPlayersList.size();
                updateItemLore(stack, remainingPlayers);
            }
        }
        SuitcaseBlockEntity.removeSuitcaseEntry(keystoneName, player.getStringUUID(), player.level().getServer()
        );
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!world.isClientSide() && entity instanceof ServerPlayer player) {
            String currentDimension = world.dimension().identifier().getPath();
            if (currentDimension.startsWith("pocket_dimension_")) {
                String keystoneName = currentDimension.replace("pocket_dimension_", "");
                preparePlayerForTeleport(player);
                world.playSound(null, pos, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 2.0f, 1.0f);
                ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
                if (overworld == null) return;

                boolean teleported = false;

                // Method 1: Try to teleport to the original suitcase block entity
                teleported = attemptSuitcaseTeleport(world, overworld, player, keystoneName);

                // Method 2: Try to find suitcase in a player's inventory
                if (!teleported) {
                    teleported = attemptPlayerInventorySuitcaseTeleport(world, overworld, player, keystoneName);
                }

                // Method 3: Try to find the suitcase as an item entity in the world
                if (!teleported) {
                    teleported = attemptSuitcaseItemTeleport(world, overworld, player, keystoneName);
                }

                // Method 4: Try to use player's last known position
                if (!teleported) {
                    teleported = attemptLastKnownPositionTeleport(world, overworld, player);
                }

                // Fallback: Take them to spawn
                if (!teleported) {
                    player.sendOverlayMessage(Component.literal("§c...").withStyle(ChatFormatting.RED));
                    BlockPos spawnPos = overworld.getRespawnData().pos();
                    teleportToPosition(world, player, overworld,
                            spawnPos.getX() + 0.5,
                            spawnPos.getY() + 1.0,
                            spawnPos.getZ() + 0.5, 0, 0);
                }
                SuitcaseBlockEntity.removeSuitcaseEntry(keystoneName, player.getStringUUID(), world.getServer());
                LAST_KNOWN_POSITIONS.remove(player.getStringUUID());
            }
        }
    }

    private void preparePlayerForTeleport(ServerPlayer player) {
        player.stopRiding();
        player.hurtMarked = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0f;
    }

    // Method 1: Try to teleport to the original suitcase block entity
    private boolean attemptSuitcaseTeleport(Level world, ServerLevel overworld, ServerPlayer player, String keystoneName) {
        BlockPos suitcasePos = SuitcaseBlockEntity.findSuitcasePosition(keystoneName, player.getStringUUID());
        if (suitcasePos == null) return false;
        ChunkPos suitcaseChunkPos = new ChunkPos(suitcasePos.getX() >> 4, suitcasePos.getZ() >> 4);
        overworld.setChunkForced(suitcaseChunkPos.x(), suitcaseChunkPos.z(), true);
        try {
            LevelChunk suitcaseChunk = overworld.getChunk(suitcaseChunkPos.x(), suitcaseChunkPos.z());
            BlockEntity targetEntity = suitcaseChunk.getBlockEntity(suitcasePos);
            if (targetEntity instanceof SuitcaseBlockEntity suitcase) {
                SuitcaseBlockEntity.EnteredPlayerData exitData = suitcase.getExitPosition(player.getStringUUID());
                if (exitData != null) {
                    teleportToPosition(world, player, overworld, exitData.x, exitData.y, exitData.z, exitData.yaw, player.getXRot());
                    world.getServer().execute(() -> {
                        overworld.setChunkForced(suitcaseChunkPos.x(), suitcaseChunkPos.z(), false);
                    });
                    return true;
                }
            }
        } finally {
            overworld.setChunkForced(suitcaseChunkPos.x(), suitcaseChunkPos.z(), false);
        }
        return false;
    }

    // Method 2: Try to find the suitcase as an item entity in the world
    private boolean attemptSuitcaseItemTeleport(Level world, ServerLevel overworld, ServerPlayer player, String keystoneName) {
        BlockPos searchCenter = null;
        BlockPos suitcasePos = SuitcaseBlockEntity.findSuitcasePosition(keystoneName, player.getStringUUID());
        if (suitcasePos != null) {
            searchCenter = suitcasePos;
        } else {
            PlayerPositionData lastPos = LAST_KNOWN_POSITIONS.get(player.getStringUUID());
            if (lastPos != null) {
                searchCenter = new BlockPos((int)lastPos.x, (int)lastPos.y, (int)lastPos.z);
            } else {
                searchCenter = overworld.getRespawnData().pos();
            }
        }
        int centerX = searchCenter.getX() >> 4;
        int centerZ = searchCenter.getZ() >> 4;
        for (int radius = 0; radius <= SEARCH_RADIUS_CHUNKS; radius++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    if (radius > 0 && x > centerX - radius && x < centerX + radius &&
                            z > centerZ - radius && z < centerZ + radius) {
                        continue;
                    }
                    if (!overworld.hasChunk(x, z)) {
                        continue;
                    }
                    LevelChunk chunk = overworld.getChunk(x, z);
                    List<ItemEntity> itemEntities = overworld.getEntitiesOfClass(
                            ItemEntity.class,
                            new AABB(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(),
                                    chunk.getPos().getMaxBlockX(), 256, chunk.getPos().getMaxBlockZ()),
                            itemEntity -> {
                                ItemStack stack = itemEntity.getItem();
                                CompoundTag beTag = getSuitcaseData(stack);
                                if (beTag == null) return false;

                                return keystoneName.equals(beTag.getStringOr("BoundKeystone", ""));
                            }
                    );
                    if (!itemEntities.isEmpty()) {
                        ItemEntity suitcaseItem = itemEntities.get(0);
                        cleanUpSuitcaseItemNbt(suitcaseItem, player, keystoneName);
                        teleportToPosition(world, player, overworld,
                                suitcaseItem.getX(), suitcaseItem.getY() + 1.0, suitcaseItem.getZ(),
                                player.getYRot(), player.getXRot());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Method 3: Try to use player's last known position
    private boolean attemptLastKnownPositionTeleport(Level world, ServerLevel overworld, ServerPlayer player) {
        PlayerPositionData lastPos = LAST_KNOWN_POSITIONS.get(player.getStringUUID());
        if (lastPos != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPos.timestamp > 10 * 60 * 1000) {
                return false;
            }
            player.sendOverlayMessage(Component.literal("§6Returning to your last known position."));
            teleportToPosition(world, player, overworld,
                    lastPos.x, lastPos.y, lastPos.z,
                    lastPos.yaw, lastPos.pitch);
            return true;
        }
        return false;
    }

    private void teleportToPosition(Level world, ServerPlayer player, ServerLevel targetWorld,
                                    double x, double y, double z, float yaw, float pitch) {
        player.teleport(new TeleportTransition(
                targetWorld, new Vec3(x, y, z), Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING
        ));
    }

    private void cleanUpSuitcaseItemNbt(ItemEntity suitcaseItem, ServerPlayer player, String keystoneName) {
        ItemStack stack = suitcaseItem.getItem();
        CompoundTag beTag = getSuitcaseData(stack);
        if (beTag == null) return;
        ListTag playersList = beTag.getList("EnteredPlayers").orElse(null);
        if (playersList != null) {
            ListTag newPlayersList = new ListTag();
            boolean playerFound = false;
            for (int i = 0; i < playersList.size(); i++) {
                CompoundTag playerData = playersList.getCompoundOrEmpty(i);
                if (!player.getStringUUID().equals(playerData.getStringOr("UUID", ""))) {
                    newPlayersList.add(playerData);
                } else {
                    playerFound = true;
                }
            }
            if (playerFound) {
                beTag.put("EnteredPlayers", newPlayersList);
                setSuitcaseData(stack, beTag);
                int remainingPlayers = newPlayersList.size();
                updateItemLore(stack, remainingPlayers);
                suitcaseItem.setItem(stack);
            }
        }
        SuitcaseBlockEntity.removeSuitcaseEntry(keystoneName, player.getStringUUID(), player.level().getServer());
    }

    private void updateItemLore(ItemStack stack, int playerCount) {
        ItemLore oldLore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        List<Component> newLore = new java.util.ArrayList<>();
        for (Component line : oldLore.lines()) {
            if (!line.getString().toLowerCase().contains("traveler")) {
                newLore.add(line);
            }
        }
        if (playerCount > 0) {
            newLore.add(0, Component.literal("§c⚠ Contains " + playerCount + " traveler(s)!")
                    .withStyle(ChatFormatting.RED));
        }
        stack.set(DataComponents.LORE, new ItemLore(newLore));
    }

    private static CompoundTag getSuitcaseData(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.type() != ModBlockEntities.SUITCASE_BLOCK_ENTITY) {
            return null;
        }
        return data.copyTagWithoutId();
    }

    private static void setSuitcaseData(ItemStack stack, CompoundTag data) {
        stack.set(DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.of(ModBlockEntities.SUITCASE_BLOCK_ENTITY, data));
    }
}
