package com.beckytidus.soldiermod.registry;

import net.minecraft.util.RandomSource;

import java.util.List;

/** Callsigns a soldier can be issued at enlistment. */
public final class SoldierNames {
    private static final List<String> NAMES = List.of(
        "Able", "Baker", "Charlie", "Dog", "Easy", "Fox", "George", "How",
        "Item", "Jig", "King", "Love", "Mike", "Nan", "Oboe", "Peter",
        "Queen", "Roger", "Sugar", "Tare", "Uncle", "Victor", "William", "Xray",
        "Yoke", "Zebra", "Hawk", "Reaper", "Ghost", "Bishop", "Cobra", "Viper",
        "Rook", "Kilo", "Bravo", "Delta", "Echo", "Foxtrot", "Golf", "Hotel",
        "India", "Juliet", "Lima", "November", "Oscar", "Papa", "Romeo", "Tango"
    );

    private SoldierNames() {}

    public static String pick(RandomSource random) {
        return NAMES.get(random.nextInt(NAMES.size()));
    }
}
