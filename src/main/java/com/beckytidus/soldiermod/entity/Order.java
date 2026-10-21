package com.beckytidus.soldiermod.entity;

import java.util.Locale;

/**
 * A standing order. A soldier is defined by the order it is carrying out, and
 * every order is a real behaviour: where it walks, what it shoots at and
 * whether it holds ground or gives chase.
 */
public enum Order {
    FOLLOW("Following", false, false),
    HOLD("Holding Position", false, true),
    GUARD("Guarding", true, true),
    PATROL("Patrolling", true, true),
    CHARGE("Charging", true, false),
    REGROUP("Regrouping", false, false),
    RETREAT("Retreating", false, false);

    private final String label;
    private final boolean aggressive;
    private final boolean anchored;

    Order(String label, boolean aggressive, boolean anchored) {
        this.label = label;
        this.aggressive = aggressive;
        this.anchored = anchored;
    }

    public String label() {
        return label;
    }

    /** True when the soldier should seek out enemies on its own. */
    public boolean isAggressive() {
        return aggressive;
    }

    /** True when the soldier fights around its post instead of roaming free. */
    public boolean isAnchored() {
        return anchored;
    }

    public Order next() {
        Order[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** Parses an order name or a spoken command. Returns null when nothing matches. */
    public static Order tryParse(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (Order o : values()) {
            if (o.name().toLowerCase(Locale.ROOT).equals(n) || o.label.toLowerCase(Locale.ROOT).equals(n)) {
                return o;
            }
        }
        return switch (n) {
            case "follow", "come", "heel", "with", "formation", "onme", "me" -> FOLLOW;
            case "hold", "stay", "wait", "stop", "halt", "post", "position", "dig" -> HOLD;
            case "guard", "defend", "protect", "watch", "secure", "cover" -> GUARD;
            case "patrol", "sweep", "scout", "route", "walk", "recon" -> PATROL;
            case "charge", "attack", "assault", "engage", "advance", "kill", "fire" -> CHARGE;
            case "regroup", "rally", "assemble", "muster", "formup", "report" -> REGROUP;
            case "retreat", "fallback", "withdraw", "flee", "run", "disengage", "bail" -> RETREAT;
            default -> null;
        };
    }

    public static Order byName(String name) {
        Order parsed = tryParse(name);
        return parsed != null ? parsed : FOLLOW;
    }
}
