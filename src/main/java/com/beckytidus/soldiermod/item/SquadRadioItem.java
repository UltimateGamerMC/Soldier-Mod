package com.beckytidus.soldiermod.item;

import com.beckytidus.soldiermod.entity.Formation;
import com.beckytidus.soldiermod.entity.Order;
import com.beckytidus.soldiermod.entity.SoldierEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * The squad radio. Right-click the air to recall everyone to your position;
 * sneak + right-click to change the formation the squad moves in.
 * Right-click a soldier to hand it the next order.
 */
public class SquadRadioItem extends Item {
    private static final double RECALL_RANGE = 160.0;

    public SquadRadioItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return InteractionResult.SUCCESS;
        List<SoldierEntity> squad = level.getEntitiesOfClass(SoldierEntity.class,
                user.getBoundingBox().inflate(RECALL_RANGE), s -> s.isOwnedBy(user) && s.isAlive());
        if (squad.isEmpty()) {
            user.sendSystemMessage(Component.literal("\u2694 No soldiers in your squad nearby."));
            return InteractionResult.SUCCESS;
        }

        if (user.isShiftKeyDown()) {
            Formation next = squad.getFirst().getFormation().next();
            for (SoldierEntity soldier : squad) soldier.setFormation(next);
            user.sendSystemMessage(Component.literal("\u2694 Squad formation: " + next.label() + "."));
            return InteractionResult.SUCCESS;
        }

        for (SoldierEntity soldier : squad) {
            soldier.setOrder(Order.REGROUP, false);
            soldier.snapTo(user.getX(), user.getY(), user.getZ(), user.getYRot(), 0);
        }
        user.sendSystemMessage(Component.literal("\u2694 Recalled " + squad.size() + " soldier(s)."));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag type) {
        out.accept(Component.literal("Right-click the air: recall the squad").withStyle(ChatFormatting.GOLD));
        out.accept(Component.literal("Sneak + right-click: change formation").withStyle(ChatFormatting.GRAY));
        out.accept(Component.literal("Right-click a soldier: next order").withStyle(ChatFormatting.GRAY));
    }
}
