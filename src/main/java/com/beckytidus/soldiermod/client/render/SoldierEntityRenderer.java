package com.beckytidus.soldiermod.client.render;

import com.beckytidus.soldiermod.SoldierMod;
import com.beckytidus.soldiermod.entity.SoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

@Environment(EnvType.CLIENT)
public class SoldierEntityRenderer extends HumanoidMobRenderer<SoldierEntity, AvatarRenderState, PlayerModel> {
    private static final int TEXTURE_COUNT = 6;

    public SoldierEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(SoldierEntity entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = builtInSkin(entity.getTextureVariant());
    }

    private static PlayerSkin builtInSkin(String variant) {
        Identifier tex = Identifier.fromNamespaceAndPath(SoldierMod.MOD_ID, "entity/soldier_" + variantFor(variant));
        return new PlayerSkin(new ClientAsset.ResourceTexture(tex), null, null, PlayerModelType.WIDE, false);
    }

    private static int variantFor(String variant) {
        if (variant != null && !variant.isEmpty()) {
            try {
                int n = Integer.parseInt(variant);
                if (n >= 1 && n <= TEXTURE_COUNT) return n;
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 1;
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poseStack) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }
}
