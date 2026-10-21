package com.beckytidus.soldiermod.interaction;

import com.beckytidus.soldiermod.entity.SoldierEntity;
import com.beckytidus.soldiermod.entity.SoldierSpeech;
import com.beckytidus.soldiermod.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Right-clicking a soldier. No menus: you give orders, hand over a weapon,
 * feed them or rename them, the way you would address a real squad.
 */
public class SoldierInteractionHandler {
    public static InteractionResult handle(Player player, SoldierEntity soldier) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean client = player.level().isClientSide();
        boolean isOwner = soldier.isOwnedBy(player);

        if (stack.isEmpty()) {
            if (!client) {
                if (player instanceof ServerPlayer sp) soldier.reportTo(sp, true);
                if (player.isShiftKeyDown() && isOwner) {
                    soldier.setOrder(soldier.getOrder().next());
                } else if (isOwner) {
                    soldier.speak(SoldierSpeech.idle(player.level().getRandom()));
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(ItemRegistry.SQUAD_RADIO) && isOwner) {
            if (!client) soldier.setOrder(soldier.getOrder().next());
            return InteractionResult.SUCCESS;
        }

        if (isFood(stack)) {
            if (!client) {
                soldier.heal(6.0f);
                player.sendSystemMessage(Component.literal("\u2694 " + soldier.getSoldierName() + ": Thank you, commander."));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }

        if (isWeapon(stack) && isOwner) {
            if (!client) {
                soldier.equipWeapon(stack);
                player.sendSystemMessage(Component.literal("\u2694 " + soldier.getSoldierName() + ": A fine weapon. I'll put it to work."));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.NAME_TAG) && isOwner) {
            if (!client) {
                String name = stack.getHoverName().getString();
                soldier.setSoldierName(name);
                player.sendSystemMessage(Component.literal("\u2694 Soldier renamed to " + name + "."));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
    }

    private static boolean isFood(ItemStack stack) {
        return stack.is(Items.BREAD) || stack.is(Items.COOKED_BEEF) || stack.is(Items.COOKED_PORKCHOP)
                || stack.is(Items.COOKED_CHICKEN) || stack.is(Items.COOKED_MUTTON) || stack.is(Items.COOKED_COD)
                || stack.is(Items.COOKED_SALMON) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.APPLE)
                || stack.is(Items.BAKED_POTATO) || stack.is(Items.COOKIE) || stack.is(Items.PUMPKIN_PIE);
    }
}
