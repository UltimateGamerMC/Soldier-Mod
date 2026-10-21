package com.beckytidus.soldiermod.entity;

import net.minecraft.util.RandomSource;

/** Everything a soldier says. Barked, clipped, military. */
public final class SoldierSpeech {

    private static final String[] ACKNOWLEDGE = {
        "Sir, yes sir!", "Orders received.", "Moving out.", "On it, commander.",
        "Acknowledged.", "Understood. Moving."
    };
    private static final String[] IDLE = {
        "Awaiting orders, sir.", "All quiet on this front.", "Ready and willing.",
        "Standing by.", "Weapons hot, commander.", "Say the word."
    };
    private static final String[] ENGAGING = {
        "Contact!", "Enemy spotted - engaging!", "Hostile down range!", "Tango in sight!"
    };
    private static final String[] KILL = {
        "Target down.", "Hostile neutralised.", "Scratch one.", "That's another one for the squad."
    };
    private static final String[] HURT = {
        "Taking fire!", "Man down over here!", "I'm hit!", "Need support!"
    };
    private static final String[] RETREAT = {
        "Falling back!", "Withdrawing!", "Pulling out, sir!", "Disengaging!"
    };
    private static final String[] RECALL = {
        "On my way, sir.", "Regrouping!", "Coming to you.", "Moving to your position."
    };
    private static final String[] LAST_STAND = {
        "It's been an honour, sir...", "Tell them we held the line...", "Going down... hold the position..."
    };

    private SoldierSpeech() {}

    public static String acknowledge(RandomSource r) { return pick(ACKNOWLEDGE, r); }
    public static String idle(RandomSource r) { return pick(IDLE, r); }
    public static String engaging(RandomSource r) { return pick(ENGAGING, r); }
    public static String kill(RandomSource r) { return pick(KILL, r); }
    public static String hurt(RandomSource r) { return pick(HURT, r); }
    public static String retreat(RandomSource r) { return pick(RETREAT, r); }
    public static String recall(RandomSource r) { return pick(RECALL, r); }
    public static String lastStand(RandomSource r) { return pick(LAST_STAND, r); }

    private static String pick(String[] pool, RandomSource r) {
        return pool[r.nextInt(pool.length)];
    }
}
