package net.bennyboops.modid.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.Vec3;

public class MobEntryData extends SavedData {
    private static final String DATA_KEY = "pocket_entry_data";
    private static final Codec<MobEntryData> CODEC = CompoundTag.CODEC.xmap(
            MobEntryData::fromNbt,
            data -> data.save(new CompoundTag())
    );
    private static final SavedDataType<MobEntryData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("pocket-repose", DATA_KEY),
            MobEntryData::new,
            CODEC,
            null
    );
    private Vec3 entryPos = Vec3.ZERO;
    private float entryYaw = 0f, entryPitch = 0f;

    public MobEntryData() {
        super();
        this.entryPos   = new Vec3(35.5, 85, 16.5);
        this.entryYaw   = 0f;
        this.entryPitch = 0f;
    }

    public static MobEntryData get(ServerLevel world) {
        SavedDataStorage mgr = world.getDataStorage();
        return mgr.computeIfAbsent(TYPE);
    }

    private static MobEntryData fromNbt(CompoundTag nbt) {
        MobEntryData data = new MobEntryData();
        data.readNbt(nbt);
        return data;
    }

    public void readNbt(CompoundTag nbt) {
        double x = nbt.getDouble("entryX").orElse(35.5);
        double y = nbt.getDouble("entryY").orElse(85.0);
        double z = nbt.getDouble("entryZ").orElse(16.5);
        this.entryPos = new Vec3(x, y, z);
        this.entryYaw   = nbt.getFloat("entryYaw").orElse(0.0F);
        this.entryPitch = nbt.getFloat("entryPitch").orElse(0.0F);
    }

    public CompoundTag save(CompoundTag nbt) {
        nbt.putDouble("entryX",   entryPos.x);
        nbt.putDouble("entryY",   entryPos.y);
        nbt.putDouble("entryZ",   entryPos.z);
        nbt.putFloat( "entryYaw",   entryYaw);
        nbt.putFloat( "entryPitch", entryPitch);
        return nbt;
    }

    public void setEntry(Vec3 pos, float yaw, float pitch) {
        this.entryPos   = pos;
        this.entryYaw   = yaw;
        this.entryPitch = pitch;
        this.setDirty();
    }

    public Vec3 getEntryPos()   { return entryPos; }
    public float getEntryYaw()    { return entryYaw; }
    public float getEntryPitch()  { return entryPitch; }
}
