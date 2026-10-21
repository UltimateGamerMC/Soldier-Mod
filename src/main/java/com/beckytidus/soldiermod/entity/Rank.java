package com.beckytidus.soldiermod.entity;

import java.util.Locale;

/**
 * Rank is earned on the battlefield: every enemy a soldier puts down counts
 * toward the next stripe, and each rank makes it tougher and hit harder.
 */
public enum Rank {
    PRIVATE("Private", 0, 0.0, 0.0),
    CORPORAL("Corporal", 5, 4.0, 1.0),
    SERGEANT("Sergeant", 15, 8.0, 2.0),
    CAPTAIN("Captain", 30, 14.0, 4.0);

    private final String label;
    private final int killsRequired;
    private final double bonusHealth;
    private final double bonusDamage;

    Rank(String label, int killsRequired, double bonusHealth, double bonusDamage) {
        this.label = label;
        this.killsRequired = killsRequired;
        this.bonusHealth = bonusHealth;
        this.bonusDamage = bonusDamage;
    }

    public String label() {
        return label;
    }

    public int killsRequired() {
        return killsRequired;
    }

    public double bonusHealth() {
        return bonusHealth;
    }

    public double bonusDamage() {
        return bonusDamage;
    }

    /** The chevrons shown next to the name: none, one, two, three. */
    public String chevrons() {
        return switch (this) {
            case PRIVATE -> "";
            case CORPORAL -> "\u25B4";
            case SERGEANT -> "\u25B4\u25B4";
            case CAPTAIN -> "\u25B4\u25B4\u25B4";
        };
    }

    public Rank next() {
        Rank[] values = values();
        return this == CAPTAIN ? CAPTAIN : values[ordinal() + 1];
    }

    public static Rank forKills(int kills) {
        Rank best = PRIVATE;
        for (Rank rank : values()) {
            if (kills >= rank.killsRequired) best = rank;
        }
        return best;
    }

    public static Rank byName(String name) {
        if (name == null) return PRIVATE;
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (Rank rank : values()) {
            if (rank.name().toLowerCase(Locale.ROOT).equals(n) || rank.label.toLowerCase(Locale.ROOT).equals(n)) {
                return rank;
            }
        }
        return PRIVATE;
    }
}
