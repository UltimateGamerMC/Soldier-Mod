package com.beckytidus.soldiermod.command;

import com.beckytidus.soldiermod.entity.Formation;
import com.beckytidus.soldiermod.entity.Order;
import com.beckytidus.soldiermod.entity.SoldierClass;
import com.beckytidus.soldiermod.entity.SoldierEntity;
import com.beckytidus.soldiermod.item.EnlistmentPapersItem;
import com.beckytidus.soldiermod.registry.ItemRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SoldierCommand {
    private static final double FIND_RANGE = 160.0;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommand(dispatcher));
    }

    private static List<SoldierEntity> squad(ServerPlayer player) {
        return player.level().getEntitiesOfClass(SoldierEntity.class, player.getBoundingBox().inflate(FIND_RANGE),
                s -> s.isOwnedBy(player) && s.isAlive());
    }

    @Nullable
    private static SoldierEntity closest(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SoldierEntity best = null;
        double bestSq = FIND_RANGE * FIND_RANGE;
        for (SoldierEntity s : squad(player)) {
            double d = s.distanceToSqr(player);
            if (d < bestSq) {
                bestSq = d;
                best = s;
            }
        }
        return best;
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("soldier")
                .then(Commands.literal("recruit")
                    .executes(ctx -> recruit(ctx.getSource(), SoldierClass.RIFLEMAN))
                    .then(Commands.argument("class", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (SoldierClass c : SoldierClass.values()) builder.suggest(c.name().toLowerCase());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> recruit(ctx.getSource(), SoldierClass.byName(StringArgumentType.getString(ctx, "class"))))
                    )
                )
                .then(Commands.literal("order")
                    .then(Commands.argument("order", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Order o : Order.values()) builder.suggest(o.name().toLowerCase());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> order(ctx.getSource(), StringArgumentType.getString(ctx, "order"), false))
                        .then(Commands.literal("all")
                            .executes(ctx -> order(ctx.getSource(), StringArgumentType.getString(ctx, "order"), true)))
                    )
                )
                .then(Commands.literal("class")
                    .then(Commands.argument("class", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (SoldierClass c : SoldierClass.values()) builder.suggest(c.name().toLowerCase());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            SoldierEntity soldier = closest(ctx.getSource());
                            if (soldier == null) {
                                ctx.getSource().sendFailure(Component.literal("No soldier nearby."));
                                return 0;
                            }
                            SoldierClass chosen = SoldierClass.byName(StringArgumentType.getString(ctx, "class"));
                            soldier.setSoldierClass(chosen);
                            ctx.getSource().sendSuccess(() -> Component.literal("\u2694 " + soldier.getSoldierName() + " re-trained as a " + chosen.label() + "."), false);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("formation")
                    .then(Commands.argument("formation", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Formation f : Formation.values()) builder.suggest(f.name().toLowerCase());
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Formation chosen = Formation.byName(StringArgumentType.getString(ctx, "formation"));
                            List<SoldierEntity> list = squad(player);
                            for (SoldierEntity s : list) s.setFormation(chosen);
                            ctx.getSource().sendSuccess(() -> Component.literal("\u2694 Squad formation: " + chosen.label() + " (" + list.size() + " soldiers)."), false);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        List<SoldierEntity> list = squad(player);
                        ctx.getSource().sendSuccess(() -> Component.literal("\u2694 Squad: " + list.size() + " soldier(s)."), false);
                        for (SoldierEntity s : list) ctx.getSource().sendSuccess(s::getStatusText, false);
                        return 1;
                    })
                )
                .then(Commands.literal("recall")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        List<SoldierEntity> list = squad(player);
                        for (SoldierEntity s : list) {
                            s.setOrder(Order.REGROUP, false);
                            s.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
                        }
                        ctx.getSource().sendSuccess(() -> Component.literal("\u2694 Recalled " + list.size() + " soldier(s)."), false);
                        return 1;
                    })
                )
                .then(Commands.literal("dismiss")
                    .executes(ctx -> {
                        SoldierEntity soldier = closest(ctx.getSource());
                        if (soldier == null) {
                            ctx.getSource().sendFailure(Component.literal("No soldier nearby."));
                            return 0;
                        }
                        soldier.speak("Honourable discharge. Good luck, commander.");
                        soldier.discard();
                        ctx.getSource().sendSuccess(() -> Component.literal("\u2694 Soldier discharged."), false);
                        return 1;
                    })
                )
                .then(Commands.literal("give")
                    .executes(ctx -> give(ctx.getSource(), ItemRegistry.ENLISTMENT_PAPERS, "Enlistment Papers"))
                    .then(Commands.literal("radio").executes(ctx -> give(ctx.getSource(), ItemRegistry.SQUAD_RADIO, "Squad Radio")))
                )
        );
    }

    private static int recruit(CommandSourceStack source, SoldierClass soldierClass) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = (ServerLevel) player.level();
        SoldierEntity soldier = EnlistmentPapersItem.recruit(level, player, soldierClass);
        source.sendSuccess(() -> Component.literal("\u2694 " + soldier.getSoldierName() + " enlisted as a " + soldierClass.label() + "."), false);
        return 1;
    }

    private static int order(CommandSourceStack source, String raw, boolean all) throws CommandSyntaxException {
        Order parsed = Order.tryParse(raw);
        if (parsed == null) {
            source.sendFailure(Component.literal("Unknown order: " + raw));
            return 0;
        }
        if (all) {
            ServerPlayer player = source.getPlayerOrException();
            List<SoldierEntity> list = squad(player);
            for (SoldierEntity s : list) s.setOrder(parsed, false);
            source.sendSuccess(() -> Component.literal("\u2694 Whole squad ordered to " + parsed.label() + " (" + list.size() + ")."), false);
            return 1;
        }
        SoldierEntity soldier = closest(source);
        if (soldier == null) {
            source.sendFailure(Component.literal("No soldier nearby."));
            return 0;
        }
        soldier.setOrder(parsed);
        return 1;
    }

    private static int give(CommandSourceStack source, Item item, String label) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = new ItemStack(item);
        if (!player.addItem(stack)) player.drop(stack, false, Prediction.SERVER_ONLY);
        source.sendSuccess(() -> Component.literal("Gave " + label + " to " + player.getName().getString()), false);
        return 1;
    }
}
