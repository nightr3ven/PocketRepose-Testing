package net.bennyboops.modid.data;

import com.mojang.serialization.Codec;
import net.bennyboops.modid.block.entity.SuitcaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import java.util.HashMap;
import java.util.Map;

public class SuitcaseRegistrySavedData extends SavedData {
    public static final String DATA_NAME = "pocket-repose:suitcase_registry";
    private static final Codec<SuitcaseRegistrySavedData> CODEC = CompoundTag.CODEC.xmap(
            SuitcaseRegistrySavedData::readNbt,
            data -> data.save(new CompoundTag())
    );
    private static final SavedDataType<SuitcaseRegistrySavedData> TYPE = new SavedDataType<>(
            Identifier.parse(DATA_NAME),
            SuitcaseRegistrySavedData::new,
            CODEC,
            null
    );
    private final Map<String, Map<String, BlockPos>> registry = new HashMap<>();

    public SuitcaseRegistrySavedData() {
        super();
    }

    public CompoundTag save(CompoundTag nbt) {
        CompoundTag top = new CompoundTag();
        for (Map.Entry<String, Map<String, BlockPos>> entry : registry.entrySet()) {
            String keystone = entry.getKey();
            Map<String, BlockPos> playerMap = entry.getValue();

            ListTag playerList = new ListTag();
            for (Map.Entry<String, BlockPos> e2 : playerMap.entrySet()) {
                CompoundTag record = new CompoundTag();
                record.putString("UUID", e2.getKey());
                BlockPos pos = e2.getValue();
                record.putInt("X", pos.getX());
                record.putInt("Y", pos.getY());
                record.putInt("Z", pos.getZ());
                playerList.add(record);
            }
            top.put(keystone, playerList);
        }
        nbt.put("RegistryEntries", top);
        return nbt;
    }

    public static SuitcaseRegistrySavedData readNbt(CompoundTag nbt) {
        SuitcaseRegistrySavedData data = new SuitcaseRegistrySavedData();
        CompoundTag top = nbt.getCompoundOrEmpty("RegistryEntries");
        for (String keystone : top.keySet()) {
            ListTag playerList = top.getListOrEmpty(keystone);
            Map<String, BlockPos> playerMap = new HashMap<>();
            for (int i = 0; i < playerList.size(); i++) {
                CompoundTag rec = playerList.getCompoundOrEmpty(i);
                String uuid = rec.getString("UUID").orElse("");
                int x = rec.getInt("X").orElse(0);
                int y = rec.getInt("Y").orElse(0);
                int z = rec.getInt("Z").orElse(0);
                if (!uuid.isEmpty()) {
                    playerMap.put(uuid, new BlockPos(x, y, z));
                }
            }
            data.registry.put(keystone, playerMap);
        }
        return data;
    }

    public void syncFromStaticRegistry() {
        registry.clear();
        SuitcaseBlockEntity.saveSuitcaseRegistryTo(registry);
        setDirty();
    }

    public void syncToStaticRegistry() {
        SuitcaseBlockEntity.initializeSuitcaseRegistry(registry);
    }

    public static void onServerStart(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        SavedDataStorage mgr = overworld.getDataStorage();
        SuitcaseRegistrySavedData data = mgr.computeIfAbsent(TYPE);
        data.syncToStaticRegistry();
    }

    public static void onRegistryChanged(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        SavedDataStorage mgr = overworld.getDataStorage();
        SuitcaseRegistrySavedData data = mgr.computeIfAbsent(TYPE);
        data.syncFromStaticRegistry();
    }
}
