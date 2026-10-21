package com.beckytidus.soldiermod.registry;

import com.beckytidus.soldiermod.SoldierMod;
import com.beckytidus.soldiermod.item.EnlistmentPapersItem;
import com.beckytidus.soldiermod.item.SquadRadioItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ItemRegistry {
    public static Item ENLISTMENT_PAPERS;
    public static Item SQUAD_RADIO;

    public static void register() {
        ENLISTMENT_PAPERS = register("enlistment_papers", EnlistmentPapersItem::new);
        SQUAD_RADIO = register("squad_radio", SquadRadioItem::new);
    }

    private static Item register(String path, Function<Item.Properties, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(SoldierMod.MOD_ID, path);
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        Item.Properties settings = new Item.Properties().stacksTo(1).setId(key);
        return Registry.register(BuiltInRegistries.ITEM, id, factory.apply(settings));
    }
}
