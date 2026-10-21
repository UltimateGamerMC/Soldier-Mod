package com.beckytidus.soldiermod.registry;

import com.beckytidus.soldiermod.SoldierMod;
import com.beckytidus.soldiermod.entity.SoldierEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class EntityRegistry {
    public static EntityType<SoldierEntity> SOLDIER;

    public static void register() {
        Identifier id = Identifier.fromNamespaceAndPath(SoldierMod.MOD_ID, "soldier");
        ResourceKey<EntityType<?>> key = ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), id);

        SOLDIER = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id,
                EntityType.Builder.of(SoldierEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.9f)
                        .build(key)
        );
    }
}
