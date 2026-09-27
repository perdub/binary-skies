package com.alisk.binaryskies;

public enum PlanetPhase {
    DAY("Day"),
    SUNSET("Sunset"),
    NIGHT("Night"),
    DAWN("Dawn");

    public final String displayName;

    PlanetPhase(String displayName) {
        this.displayName = displayName;
    }
}
