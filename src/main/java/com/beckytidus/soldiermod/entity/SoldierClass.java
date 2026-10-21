package com.beckytidus.soldiermod.entity;

import java.util.Locale;

/**
 * A soldier's specialisation. Classes trade stats against each other and each
 * one brings a battlefield ability of its own.
 */
public enum SoldierClass {
    RIFLEMAN("Rifleman", 1.00, 2.0, 0.0, 0.0, 0.0),
    MEDIC("Medic", 1.00, -1.0, 0.0, 0.0, 0.0),
    HEAVY("Heavy", 0.86, 1.0, 8.0, 6.0, 0.6),
    SCOUT("Scout", 1.28, 0.0, 0.0, 0.0, 0.0);

    private final String label;
    private final double speedMultiplier;
    private final double bonusDamage;
    private final double bonusArmor;
    private final double bonusHealth;
    private final double bonusKnockbackResistance;

    SoldierClass(String label, double speedMultiplier, double bonusDamage, double bonusArmor,
                 double bonusHealth, double bonusKnockbackResistance) {
        this.label = label;
        this.speedMultiplier = speedMultiplier;
        this.bonusDamage = bonusDamage;
        this.bonusArmor = bonusArmor;
        this.bonusHealth = bonusHealth;
        this.bonusKnockbackResistance = bonusKnockbackResistance;
    }

    public String label() {
        return label;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public double bonusDamage() {
        return bonusDamage;
    }

    public double bonusArmor() {
        return bonusArmor;
    }

    public double bonusHealth() {
        return bonusHealth;
    }

    public double bonusKnockbackResistance() {
        return bonusKnockbackResistance;
    }

    /** How far a scout can spot enemies, and how far a medic reaches with aid. */
    public double bonusFollowRange() {
        return this == SCOUT ? 16.0 : 0.0;
    }

    /** The medic patches up the squad; the scout paints targets. */
    public boolean hasFieldAbility() {
        return this == MEDIC || this == SCOUT;
    }

    public SoldierClass next() {
        SoldierClass[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static SoldierClass byName(String name) {
        if (name == null) return RIFLEMAN;
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (SoldierClass c : values()) {
            if (c.name().toLowerCase(Locale.ROOT).equals(n) || c.label.toLowerCase(Locale.ROOT).equals(n)) {
                return c;
            }
        }
        return switch (n) {
            case "rifle", "soldier", "infantry", "grunt" -> RIFLEMAN;
            case "med", "doctor", "corpsman" -> MEDIC;
            case "tank", "juggernaut", "shield", "gunner" -> HEAVY;
            case "sniper", "spotter", "ranger" -> SCOUT;
            default -> RIFLEMAN;
        };
    }
}
