package com.beckytidus.soldiermod.client.hud;

import com.beckytidus.soldiermod.entity.SoldierEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Two readouts: a squad roster down the left edge whenever you command soldiers,
 * and a detail card under the crosshair for the soldier you are looking at.
 */
public final class SquadHud {
    private static final int MAX_ROSTER = 8;

    private SquadHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.hud.isHidden() || minecraft.player == null || minecraft.gui.screen() != null) return;

        roster(graphics, minecraft);
        targetCard(graphics, minecraft);
    }

    private static void roster(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (minecraft.level == null) return;
        List<SoldierEntity> squad = minecraft.level.getEntitiesOfClass(SoldierEntity.class,
                minecraft.player.getBoundingBox().inflate(128.0), s -> s.isAlive() && s.isOwnedBy(minecraft.player));
        if (squad.isEmpty()) return;
        squad.sort(Comparator.comparingDouble(s -> s.distanceToSqr(minecraft.player)));

        int shown = Math.min(MAX_ROSTER, squad.size());
        int width = 0;
        for (int i = 0; i < shown; i++) {
            width = Math.max(width, minecraft.font.width(rosterLine(squad.get(i))));
        }
        width += 10;
        int x0 = 4;
        int y0 = 6;

        graphics.fill(x0, y0 - 2, x0 + width, y0 + 4 + shown * 10, 0x90101810);
        graphics.text(minecraft.font, Component.literal("\u2694 SQUAD  " + squad.size()).withColor(0xFFD766),
                x0 + 5, y0, 0xFFFFFFFF, true);
        for (int i = 0; i < shown; i++) {
            SoldierEntity s = squad.get(i);
            String line = rosterLine(s);
            int color = s.isEngaged() ? 0xFFFF8080 : 0xFFCFE3CF;
            graphics.text(minecraft.font, line, x0 + 5, y0 + 12 + i * 10, color, true);
        }
    }

    private static String rosterLine(SoldierEntity s) {
        String chevrons = s.getRank().chevrons();
        String name = s.getSoldierName() + (chevrons.isEmpty() ? "" : " " + chevrons);
        return name + "  " + s.getSoldierClass().label() + "  -  " + s.getOrder().label();
    }

    private static void targetCard(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (!(minecraft.crosshairPickEntity instanceof SoldierEntity soldier) || !soldier.isAlive()) return;
        if (soldier.distanceToSqr(minecraft.player) > 8 * 8) return;

        int w = minecraft.getWindow().getGuiScaledWidth();
        int y = 6;
        Component title = Component.literal(soldier.getTagName()).withColor(0xFFD766);
        String line = soldier.getOrder().label() + "   Kills " + soldier.getKills()
                + "   " + (int) soldier.getHealth() + "/" + (int) soldier.getMaxHealth() + " HP";
        String hint = soldier.isOwnedBy(minecraft.player)
                ? "Sneak + right-click: next order"
                : "Not yours to command";

        int width = Math.max(minecraft.font.width(title), Math.max(minecraft.font.width(line), minecraft.font.width(hint))) + 10;
        int x0 = (w - width) / 2;
        graphics.fill(x0, y - 3, x0 + width, y + 31, 0x90180F1C);
        graphics.text(minecraft.font, title, (w - minecraft.font.width(title)) / 2, y, 0xFFFFFFFF, true);
        graphics.text(minecraft.font, line, (w - minecraft.font.width(line)) / 2, y + 10, 0xFFFFFFFF, true);
        graphics.text(minecraft.font, hint, (w - minecraft.font.width(hint)) / 2, y + 20, 0xFFB8A9BE, false);
    }
}
