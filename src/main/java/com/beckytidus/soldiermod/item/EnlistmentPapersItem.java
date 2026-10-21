package com.beckytidus.soldiermod.item;

import com.beckytidus.soldiermod.entity.SoldierClass;
import com.beckytidus.soldiermod.entity.SoldierEntity;
import com.beckytidus.soldiermod.registry.EntityRegistry;
import com.beckytidus.soldiermod.registry.SoldierNames;
import com.beckytidus.soldiermod.registry.SoldierSkins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Enlists a soldier. Right-click to swear one in as the selected class,
 * sneak + right-click to change which class the next recruit will be.
 */
public class EnlistmentPapersItem extends Item {
    private static final Map<UUID, SoldierClass> SELECTED = new ConcurrentHashMap<>();

    public EnlistmentPapersItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        if (user.isShiftKeyDown()) {
            SoldierClass next = SELECTED.getOrDefault(user.getUUID(), SoldierClass.RIFLEMAN).next();
            SELECTED.put(user.getUUID(), next);
            user.sendSystemMessage(Component.literal("\u2694 Next recruit will be a " + next.label() + "."));
            return InteractionResult.SUCCESS;
        }

        SoldierClass chosen = SELECTED.getOrDefault(user.getUUID(), SoldierClass.RIFLEMAN);
        SoldierEntity soldier = recruit(serverLevel, user, chosen);
        user.sendSystemMessage(Component.literal("\u2694 " + soldier.getSoldierName() + " enlisted as a "
                + chosen.label() + ". " + soldier.getRank().label() + "."));
        user.getItemInHand(hand).consume(1, user);
        return InteractionResult.SUCCESS;
    }

    /** Spawns a soldier bound to the player and slots it into the squad. */
    public static SoldierEntity recruit(ServerLevel level, Player owner, SoldierClass soldierClass) {
        SoldierEntity soldier = new SoldierEntity(EntityRegistry.SOLDIER, level);
        soldier.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0);
        soldier.setOwner(owner);
        soldier.setSoldierName(SoldierNames.pick(level.getRandom()));
        soldier.setTextureVariant(SoldierSkins.pick(level.getRandom()));
        soldier.setSoldierClass(soldierClass);
        soldier.setSquadIndex(soldier.squadSize(owner));
        soldier.setOrder(com.beckytidus.soldiermod.entity.Order.FOLLOW, false);
        level.addFreshEntity(soldier);
        return soldier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag type) {
        out.accept(Component.literal("Enlists a soldier into your squad.").withStyle(ChatFormatting.GOLD));
        out.accept(Component.literal("Right-click: recruit").withStyle(ChatFormatting.GRAY));
        out.accept(Component.literal("Sneak + right-click: change class").withStyle(ChatFormatting.GRAY));
    }
}
