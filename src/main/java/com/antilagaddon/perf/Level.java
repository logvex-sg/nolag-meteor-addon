package com.antilagaddon.perf;

/**
 * Adaptive suppression levels. {@link #Normal} performs no extra suppression, while
 * {@link #Emergency} applies the strongest (still client-only, reversible) reduction.
 */
public enum Level {
    Normal(0, "Normal"),
    Balanced(1, "Balanced"),
    Performance(2, "Performance"),
    Emergency(3, "Emergency");

    private final int severity;
    private final String title;

    Level(int severity, String title) {
        this.severity = severity;
        this.title = title;
    }

    public int severity() { return severity; }

    public String title() { return title; }

    public static Level fromSeverity(int severity) {
        for (Level level : values()) if (level.severity == severity) return level;
        return Normal;
    }
}
