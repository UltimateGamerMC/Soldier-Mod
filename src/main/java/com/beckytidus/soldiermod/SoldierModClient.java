package com.beckytidus.soldiermod;

import com.beckytidus.soldiermod.client.hud.SquadHud;
import com.beckytidus.soldiermod.client.render.SoldierEntityRenderer;
import com.beckytidus.soldiermod.registry.EntityRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class SoldierModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityRegistry.SOLDIER, SoldierEntityRenderer::new);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(SoldierMod.MOD_ID, "squad_card"), SquadHud::extract);
    }
}
