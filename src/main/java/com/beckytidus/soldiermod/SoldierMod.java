package com.beckytidus.soldiermod;

import com.beckytidus.soldiermod.command.SoldierCommand;
import com.beckytidus.soldiermod.entity.SoldierEntity;
import com.beckytidus.soldiermod.registry.EntityRegistry;
import com.beckytidus.soldiermod.registry.ItemRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoldierMod implements ModInitializer {
    public static final String MOD_ID = "soldier-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Soldier Mod...");

        EntityRegistry.register();
        ItemRegistry.register();
        FabricDefaultAttributeRegistry.register(EntityRegistry.SOLDIER, SoldierEntity.createSoldierAttributes());
        SoldierCommand.register();

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> output.accept(ItemRegistry.ENLISTMENT_PAPERS));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            output.accept(ItemRegistry.ENLISTMENT_PAPERS);
            output.accept(ItemRegistry.SQUAD_RADIO);
        });

        LOGGER.info("Soldier Mod loaded.");
    }
}
