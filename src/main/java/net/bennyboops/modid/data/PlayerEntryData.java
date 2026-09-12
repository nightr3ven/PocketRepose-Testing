package net.bennyboops.modid.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.Vec3;

public class PlayerEntryData extends SavedData {
    private static final String DATA_KEY = "pocket_player_entry";
    private static final Codec<PlayerEntryData> CODEC = CompoundTag.CODEC.xmap(
            PlayerEntryData::fromNbt,
            data -> data.save(new CompoundTag())
    );
    private static final SavedDataType<PlayerEntryData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("pocket-repose", DATA_KEY),
            PlayerEntryData::new,
            CODEC,
            null
    );

    private Vec3 entryPos = Vec3.ZERO;
    private float entryYaw = 0f, entryPitch = 0f;

    public PlayerEntryData() {
        super();
        this.entryPos   = new Vec3(17.5, 97, 9.5);
        this.entryYaw   = 0f;
        this.entryPitch = 0f;
    }

    public static PlayerEntryData get(ServerLevel world) {
        SavedDataStorage mgr = world.getDataStorage();
        return mgr.computeIfAbsent(TYPE);
    }

    private static PlayerEntryData fromNbt(CompoundTag nbt) {
        PlayerEntryData data = new PlayerEntryData();
        data.readNbt(nbt);
        return data;
    }

    public void readNbt(CompoundTag nbt) {
        this.entryPos   = new Vec3(
                nbt.getDouble("px").orElse(17.5),
                nbt.getDouble("py").orElse(97.0),
                nbt.getDouble("pz").orElse(9.5)
        );
        this.entryYaw   = nbt.getFloat("pyaw").orElse(0.0F);
        this.entryPitch = nbt.getFloat("ppitch").orElse(0.0F);
    }

    public CompoundTag save(CompoundTag nbt) {
        nbt.putDouble("px",    entryPos.x);
        nbt.putDouble("py",    entryPos.y);
        nbt.putDouble("pz",    entryPos.z);
        nbt.putFloat( "pyaw",  entryYaw);
        nbt.putFloat( "ppitch", entryPitch);
        return nbt;
    }

    public void setEntry(Vec3 pos, float yaw, float pitch) {
        this.entryPos   = pos;
        this.entryYaw   = yaw;
        this.entryPitch = pitch;
        this.setDirty();
    }

    public Vec3   getEntryPos()   { return entryPos; }
    public float   getEntryYaw()    { return entryYaw; }
    public float   getEntryPitch()  { return entryPitch; }
}
