package com.beckytidus.soldiermod.entity;

import java.util.Locale;

/**
 * How the squad lines up around its commander when following.
 */
public enum Formation {
    WEDGE("Wedge"),
    LINE("Line"),
    COLUMN("Column");

    private final String label;

    Formation(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public Formation next() {
        Formation[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Local offset for the soldier at {@code index} in a squad of {@code size}.
     * x is sideways (positive is the commander's right), z is backwards.
     */
    public double[] offset(int index, int size) {
        return switch (this) {
            case LINE -> new double[]{(index - (size - 1) / 2.0) * 1.8, 2.6};
            case COLUMN -> new double[]{0.0, 2.6 + index * 1.8};
            case WEDGE -> {
                int row = (index + 1) / 2;
                double side = (index % 2 == 0) ? -1.0 : 1.0;
                yield new double[]{side * row * 1.8, 2.2 + row * 1.4};
            }
        };
    }

    public static Formation byName(String name) {
        if (name == null) return WEDGE;
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (Formation f : values()) {
            if (f.name().toLowerCase(Locale.ROOT).equals(n) || f.label.toLowerCase(Locale.ROOT).equals(n)) {
                return f;
            }
        }
        return switch (n) {
            case "v", "arrow", "triangle" -> WEDGE;
            case "row", "wall", "rank", "shoulder" -> LINE;
            case "single", "file", "snake", "trail" -> COLUMN;
            default -> WEDGE;
        };
    }
}
