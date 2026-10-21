package com.beckytidus.soldiermod.registry;

import net.minecraft.util.RandomSource;

/** Texture variants for the soldier's uniform and camouflage. */
public final class SoldierSkins {
    private static final int TEXTURE_COUNT = 6;

    private SoldierSkins() {}

    public static String pick(RandomSource random) {
        return String.valueOf(random.nextIntBetweenInclusive(1, TEXTURE_COUNT));
    }

    public static int count() {
        return TEXTURE_COUNT;
    }
}
