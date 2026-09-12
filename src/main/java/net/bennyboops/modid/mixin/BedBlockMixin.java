package net.bennyboops.modid.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class BedBlockMixin {

    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void preventSpawnSettingInPocketDimensions(ServerPlayer.RespawnConfig respawnConfig,
                                                       boolean showMessage, CallbackInfo ci) {
        ResourceKey<Level> dimension = respawnConfig == null
                ? null
                : respawnConfig.respawnData().dimension();
        if (dimension != null &&
                dimension.identifier().getNamespace().equals("pocket-repose") &&
                dimension.identifier().getPath().startsWith("pocket_dimension_")) {
            ci.cancel();
        }
    }
}
