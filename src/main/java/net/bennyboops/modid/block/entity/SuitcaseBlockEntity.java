package net.bennyboops.modid.block.entity;

import net.bennyboops.modid.block.PocketPortalBlock;
import net.bennyboops.modid.data.SuitcaseRegistrySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class SuitcaseBlockEntity extends BlockEntity {
    private String boundKeystoneName;
    private boolean isLocked = false;
    private boolean dimensionLocked = true;
    private final List<EnteredPlayerData> enteredPlayers = new ArrayList<>();
    public static class EnteredPlayerData {
        public final String uuid;
        public final double x;
        public final double y;
        public final double z;
        public final float pitch;
        public final float yaw;
        public final BlockPos suitcasePos;
        public EnteredPlayerData(String uuid, double x, double y, double z, float pitch, float yaw, BlockPos suitcasePos) {
            this.uuid = uuid;
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.yaw = yaw;
            this.suitcasePos = suitcasePos;
        }
        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("UUID", uuid);
            nbt.putDouble("X", x);
            nbt.putDouble("Y", y);
            nbt.putDouble("Z", z);
            nbt.putFloat("Pitch", pitch);
            nbt.putFloat("Yaw", yaw);
            nbt.putInt("SuitcaseX", suitcasePos.getX());
            nbt.putInt("SuitcaseY", suitcasePos.getY());
            nbt.putInt("SuitcaseZ", suitcasePos.getZ());
            return nbt;
        }
        public static EnteredPlayerData fromNbt(CompoundTag nbt) {
            return new EnteredPlayerData(
                    nbt.getStringOr("UUID", ""),
                    nbt.getDoubleOr("X", 0.0),
                    nbt.getDoubleOr("Y", 0.0),
                    nbt.getDoubleOr("Z", 0.0),
                    nbt.getFloatOr("Pitch", 0.0F),
                    nbt.getFloatOr("Yaw", 0.0F),
                    new BlockPos(
                            nbt.getIntOr("SuitcaseX", 0),
                            nbt.getIntOr("SuitcaseY", 0),
                            nbt.getIntOr("SuitcaseZ", 0)
                    )
            );
        }

        private void save(ValueOutput output) {
            output.putString("UUID", uuid);
            output.putDouble("X", x);
            output.putDouble("Y", y);
            output.putDouble("Z", z);
            output.putFloat("Pitch", pitch);
            output.putFloat("Yaw", yaw);
            output.putInt("SuitcaseX", suitcasePos.getX());
            output.putInt("SuitcaseY", suitcasePos.getY());
            output.putInt("SuitcaseZ", suitcasePos.getZ());
        }

        private static EnteredPlayerData load(ValueInput input) {
            return new EnteredPlayerData(
                    input.getStringOr("UUID", ""),
                    input.getDoubleOr("X", 0.0),
                    input.getDoubleOr("Y", 0.0),
                    input.getDoubleOr("Z", 0.0),
                    input.getFloatOr("Pitch", 0.0F),
                    input.getFloatOr("Yaw", 0.0F),
                    new BlockPos(
                            input.getIntOr("SuitcaseX", 0),
                            input.getIntOr("SuitcaseY", 0),
                            input.getIntOr("SuitcaseZ", 0)
                    )
            );
        }
    }

    public SuitcaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUITCASE_BLOCK_ENTITY, pos, state);
    }

    public boolean canOpenInDimension(Level world) {
        if (!dimensionLocked) {
            return true;
        }
        Set<ResourceKey<Level>> allowedDimensions = Set.of(
                Level.OVERWORLD
                //World.NETHER,
                //World.END
        );
        return allowedDimensions.contains(world.dimension());
    }

    public boolean isDimensionLocked() {
        return dimensionLocked;
    }

    private static final java.util.Set<java.util.UUID> PLAYERS_WHO_ENTERED = new java.util.HashSet<>();
    public boolean isFirstTimeEntering(ServerPlayer player) {
        return !PLAYERS_WHO_ENTERED.contains(player.getUUID());
    }

    public void playerEntered(ServerPlayer player) {
        enteredPlayers.removeIf(data -> data.uuid.equals(player.getStringUUID()));

        EnteredPlayerData data = new EnteredPlayerData(
                player.getStringUUID(),
                player.getX(), player.getY(), player.getZ(),
                player.getXRot(), player.getYRot(),
                this.getBlockPos()
        );
        enteredPlayers.add(data);

        PLAYERS_WHO_ENTERED.add(player.getUUID());

        Map<String, BlockPos> suitcases = SUITCASE_REGISTRY.computeIfAbsent(
                boundKeystoneName, k -> new HashMap<>()
        );
        suitcases.put(player.getStringUUID(), this.getBlockPos());

        PocketPortalBlock.storePlayerPosition(player);

        setChanged();

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            SuitcaseRegistrySavedData.onRegistryChanged(server);
        }
    }

    public EnteredPlayerData getExitPosition(String playerUuid) {
        for (EnteredPlayerData data : enteredPlayers) {
            if (data.uuid.equals(playerUuid)) {
                EnteredPlayerData exitData = new EnteredPlayerData(
                        data.uuid,
                        this.getBlockPos().getX() + 0.5,
                        this.getBlockPos().getY() + 1.0,
                        this.getBlockPos().getZ() + 0.5,
                        data.pitch, data.yaw,
                        this.getBlockPos()
                );
                enteredPlayers.remove(data);
                setChanged();
                return exitData;
            }
        }
        return null;
    }

    public void bindKeystone(String keystoneName) {
        this.boundKeystoneName = keystoneName;
        setChanged();
    }

    public String getBoundKeystoneName() {
        return boundKeystoneName;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
        setChanged();
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (boundKeystoneName != null) {
            output.putString("BoundKeystone", boundKeystoneName);
        }
        output.putBoolean("Locked", isLocked);
        output.putBoolean("DimensionLocked", dimensionLocked);

        ValueOutput.ValueOutputList playersList = output.childrenList("EnteredPlayers");
        for (EnteredPlayerData data : enteredPlayers) {
            data.save(playersList.addChild());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        boundKeystoneName = input.getString("BoundKeystone").orElse(null);
        isLocked = input.getBooleanOr("Locked", false);
        dimensionLocked = input.getBooleanOr("DimensionLocked", true);
        enteredPlayers.clear();
        for (ValueInput playerData : input.childrenListOrEmpty("EnteredPlayers")) {
            enteredPlayers.add(EnteredPlayerData.load(playerData));
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    public static final Map<String, Map<String, BlockPos>> SUITCASE_REGISTRY = Collections.synchronizedMap(new HashMap<>());

    public static BlockPos findSuitcasePosition(String keystoneName, String playerUuid) {
        Map<String, BlockPos> suitcases = SUITCASE_REGISTRY.get(keystoneName);
        if (suitcases != null) {
            return suitcases.get(playerUuid);
        }
        return null;
    }

    public static void removeSuitcaseEntry(String keystoneName, String playerUuid, MinecraftServer server) {
        Map<String, BlockPos> suitcases = SUITCASE_REGISTRY.get(keystoneName);
        if (suitcases != null) {
            suitcases.remove(playerUuid);
            if (suitcases.isEmpty()) {
                SUITCASE_REGISTRY.remove(keystoneName);
            }
            if (server != null) {
                SuitcaseRegistrySavedData.onRegistryChanged(server);
            }
        }
    }

    public List<EnteredPlayerData> getEnteredPlayers() {
        return enteredPlayers;
    }

    public static void initializeSuitcaseRegistry(Map<String, Map<String, BlockPos>> savedRegistry) {
        SUITCASE_REGISTRY.clear();
        for (Map.Entry<String, Map<String, BlockPos>> entry : savedRegistry.entrySet()) {
            Map<String, BlockPos> players = SUITCASE_REGISTRY.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            players.putAll(entry.getValue());
        }
    }

    public static void saveSuitcaseRegistryTo(Map<String, Map<String, BlockPos>> destination) {
        destination.clear();
        for (Map.Entry<String, Map<String, BlockPos>> entry : SUITCASE_REGISTRY.entrySet()) {
            Map<String, BlockPos> players = destination.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            players.putAll(entry.getValue());
        }
    }

    public void updatePlayerSuitcasePosition(String playerUuid, BlockPos newPos) {
        for (int i = 0; i < enteredPlayers.size(); i++) {
            EnteredPlayerData data = enteredPlayers.get(i);
            if (data.uuid.equals(playerUuid)) {
                EnteredPlayerData updatedData = new EnteredPlayerData(
                        data.uuid,
                        data.x, data.y, data.z,
                        data.pitch, data.yaw,
                        newPos
                );
                enteredPlayers.set(i, updatedData);
                break;
            }
        }
        if (boundKeystoneName != null) {
            Map<String, BlockPos> suitcases = SUITCASE_REGISTRY.computeIfAbsent(
                    boundKeystoneName, k -> new HashMap<>()
            );
            suitcases.put(playerUuid, newPos);
        }
        setChanged();
    }
}
