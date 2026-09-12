package net.bennyboops.modid.item;

import net.bennyboops.modid.PocketRepose;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {
    public static final Item KEYSTONE = registerItem("keystone", KeystoneItem::new);

    private static Item registerItem(String name, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(PocketRepose.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    public static void registerModItems() {
        PocketRepose.LOGGER.info("Registering mod items for " + PocketRepose.MOD_ID);
    }
}
