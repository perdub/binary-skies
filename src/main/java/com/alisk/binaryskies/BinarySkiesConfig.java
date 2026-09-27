package com.alisk.binaryskies;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class BinarySkiesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of("config", "binary-skies.json");
    private static BinarySkiesConfig INSTANCE;

    public int minCycleSeconds = 900;
    public int maxCycleSeconds = 1200;

    public int minDaySeconds = 300;
    public int maxDaySeconds = 540;
    public int minNightSeconds = 300;
    public int maxNightSeconds = 660;
    public int minTwilightSeconds = 30;
    public int maxTwilightSeconds = 90;

    public double anomalyChancePerCycle = 0.12;
    public boolean anomaliesEnabled = true;
    public int anomalyJumpTarget = 6000;

    public boolean secondStarEnabled = true;
    public int secondStarMinPeriodSeconds = 720;
    public int secondStarMaxPeriodSeconds = 1320;
    public double secondStarYawOffsetDegrees = 38.0;
    public double secondStarMaxAltitudeDegrees = 62.0;
    public double secondStarDistance = 48.0;
    public int secondStarRefreshTicks = 2;
    public float secondStarParticleScale = 1.7f;
    public int secondStarColor = 0xFFF1B8;

    public boolean advancementsEnabled = true;

    private BinarySkiesConfig() {
    }

    public static BinarySkiesConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                INSTANCE = GSON.fromJson(Files.readString(PATH), BinarySkiesConfig.class);
                if (INSTANCE == null) INSTANCE = new BinarySkiesConfig();
            } else {
                INSTANCE = new BinarySkiesConfig();
                save();
            }
        } catch (Exception e) {
            BinarySkies.LOGGER.error("Could not load {}", PATH, e);
            INSTANCE = new BinarySkiesConfig();
        }
    }

    public static void save() {
        if (INSTANCE == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            BinarySkies.LOGGER.error("Could not save {}", PATH, e);
        }
    }

    public void validate() {
        minCycleSeconds = clamp(minCycleSeconds, 60, 86400);
        maxCycleSeconds = clamp(maxCycleSeconds, minCycleSeconds, 86400);
        minDaySeconds = clamp(minDaySeconds, 10, maxCycleSeconds);
        maxDaySeconds = clamp(maxDaySeconds, minDaySeconds, maxCycleSeconds);
        minNightSeconds = clamp(minNightSeconds, 10, maxCycleSeconds);
        maxNightSeconds = clamp(maxNightSeconds, minNightSeconds, maxCycleSeconds);
        minTwilightSeconds = clamp(minTwilightSeconds, 1, 600);
        maxTwilightSeconds = clamp(maxTwilightSeconds, minTwilightSeconds, 600);
        anomalyChancePerCycle = Math.max(0, Math.min(1, anomalyChancePerCycle));
        secondStarMinPeriodSeconds = clamp(secondStarMinPeriodSeconds, 30, 86400);
        secondStarMaxPeriodSeconds = clamp(secondStarMaxPeriodSeconds, secondStarMinPeriodSeconds, 86400);
        secondStarMaxAltitudeDegrees = Math.max(5, Math.min(85, secondStarMaxAltitudeDegrees));
        secondStarDistance = Math.max(16, Math.min(128, secondStarDistance));
        secondStarRefreshTicks = clamp(secondStarRefreshTicks, 1, 20);
        secondStarParticleScale = Math.max(0.25f, Math.min(4.0f, secondStarParticleScale));
        secondStarColor = secondStarColor & 0xFFFFFF;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "cycle=%d-%ds, day=%d-%ds, night=%d-%ds, anomaly=%.0f%%, secondStar=%s",
                minCycleSeconds, maxCycleSeconds,
                minDaySeconds, maxDaySeconds,
                minNightSeconds, maxNightSeconds,
                anomalyChancePerCycle * 100,
                secondStarEnabled);
    }
}
