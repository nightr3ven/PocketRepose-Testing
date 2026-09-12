package net.bennyboops.modid.mixin;

import net.bennyboops.modid.item.KeystoneItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public class AnvilScreenHandlerMixin {
    @Inject(method = "createResult", at = @At("TAIL"))
    private void onAnvilRenameRemoveEnchants(CallbackInfo ci) {
        // slot 2 is the output
        Slot outputSlot = ((AnvilMenu)(Object)this).slots.get(2);
        ItemStack result = outputSlot.getItem();

        // if it's our KeystoneItem with a new custom name, strip enchantments
        if (result.getItem() instanceof KeystoneItem) {

            // Renaming a keystone makes it available to bind again.
            result.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            result.remove(DataComponents.REPAIR_COST);

        }
    }
}
