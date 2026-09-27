package com.alisk.binaryskies;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.Random;

public final class PlanetCycle {

    private final ServerWorld world;
    private final Random random;
    private final SecondStarRenderer secondStarRenderer;

    private long cycleTicks;
    private long elapsedTicks;
    private long dayTicks;
    private long sunsetTicks;
    private long nightTicks;
    private long dawnTicks;
    private int debugSpeed = 1;
    private boolean paused;
    private boolean anomalyUsed;
    private PlanetPhase phase = PlanetPhase.DAY;
    private PlanetPhase previousPhase = PlanetPhase.DAY;

    private double secondStarPhase;
    private double secondStarAngularSpeed;
    private int particleTicker;

    public PlanetCycle(ServerWorld world) {
        this.world = world;
        this.random = new Random(world.getSeed() ^ 0xB17E51L);
        this.secondStarRenderer = new SecondStarRenderer();
        reset();
    }

    public void tick() {
        BinarySkiesConfig config = BinarySkiesConfig.get();
        config.validate();

        if (!paused) {
            elapsedTicks += debugSpeed;
            secondStarPhase = wrap01(secondStarPhase + secondStarAngularSpeed * debugSpeed);

            if (elapsedTicks >= cycleTicks) {
                elapsedTicks %= cycleTicks;
                rollCycle();
            }

            updatePhase();
            double dayTime = dayTimeForProgress();
            world.setTimeOfDay((long) Math.floor(dayTime));
        }

        particleTicker++;
        if (config.secondStarEnabled && particleTicker >= config.secondStarRefreshTicks) {
            particleTicker = 0;
            secondStarRenderer.render(world, secondStarPhase);
        }
    }

    public void reset() {
        elapsedTicks = 0;
        anomalyUsed = false;
        debugSpeed = 1;
        particleTicker = 0;
        rollCycle();
        secondStarPhase = random.nextDouble();
        chooseSecondStarSpeed();
        world.setTimeOfDay(0L);
        updatePhase();
    }

    private void rollCycle() {
        BinarySkiesConfig config = BinarySkiesConfig.get();
        int targetSeconds = randomBetween(config.minCycleSeconds, config.maxCycleSeconds);
        int targetTicks = targetSeconds * 20;

        int day = randomBetween(config.minDaySeconds, config.maxDaySeconds) * 20;
        int sunset = randomBetween(config.minTwilightSeconds, config.maxTwilightSeconds) * 20;
        int night = randomBetween(config.minNightSeconds, config.maxNightSeconds) * 20;
        int dawn = randomBetween(config.minTwilightSeconds, config.maxTwilightSeconds) * 20;

        int raw = day + sunset + night + dawn;
        double scale = targetTicks / (double) Math.max(raw, 1);

        dayTicks = Math.max(20, Math.round(day * scale));
        sunsetTicks = Math.max(20, Math.round(sunset * scale));
        nightTicks = Math.max(20, Math.round(night * scale));
        dawnTicks = Math.max(20, Math.round(dawn * scale));
        cycleTicks = dayTicks + sunsetTicks + nightTicks + dawnTicks;
        anomalyUsed = false;
        chooseSecondStarSpeed();
    }

    private void chooseSecondStarSpeed() {
        BinarySkiesConfig config = BinarySkiesConfig.get();
        int period = randomBetween(config.secondStarMinPeriodSeconds, config.secondStarMaxPeriodSeconds);
        secondStarAngularSpeed = 1.0 / (period * 20.0);
    }

    private int randomBetween(int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private void updatePhase() {
        long p = elapsedTicks;
        PlanetPhase next;
        if (p < dayTicks) {
            next = PlanetPhase.DAY;
        } else if (p < dayTicks + sunsetTicks) {
            next = PlanetPhase.SUNSET;
        } else if (p < dayTicks + sunsetTicks + nightTicks) {
            next = PlanetPhase.NIGHT;
        } else {
            next = PlanetPhase.DAWN;
        }

        if (next != phase) {
            previousPhase = phase;
            phase = next;
            onPhaseChanged(previousPhase, phase);
        }

        maybeRunAnomaly();
    }

    private void onPhaseChanged(PlanetPhase from, PlanetPhase to) {
        if (BinarySkiesConfig.get().advancementsEnabled
                && to == PlanetPhase.NIGHT
                && nightTicks >= 8 * 60 * 20L) {
            grantAdvancement("long_night");
        }
    }

    private void maybeRunAnomaly() {
        BinarySkiesConfig config = BinarySkiesConfig.get();
        if (!config.anomaliesEnabled || anomalyUsed || elapsedTicks < dayTicks || elapsedTicks > dayTicks + sunsetTicks) {
            return;
        }
        if (random.nextDouble() < config.anomalyChancePerCycle / Math.max(1, sunsetTicks)) {
            anomalyUsed = true;
            elapsedTicks = Math.min(cycleTicks - 1, dayTicks / 2L);
            world.setTimeOfDay(config.anomalyJumpTarget);
            if (config.advancementsEnabled) {
                grantAdvancement("sky_refuses");
                grantAdvancement("second_dawn");
            }
            world.getPlayers().forEach(player -> player.sendMessage(
                    Text.literal("The sky refuses to become night."), false));
        }
    }

    private double dayTimeForProgress() {
        long p = elapsedTicks;
        if (p < dayTicks) {
            return lerp(0, 12000, p / (double) dayTicks);
        }
        p -= dayTicks;
        if (p < sunsetTicks) {
            return lerp(12000, 14000, p / (double) sunsetTicks);
        }
        p -= sunsetTicks;
        if (p < nightTicks) {
            return lerp(14000, 22000, p / (double) nightTicks);
        }
        p -= nightTicks;
        return lerp(22000, 24000, p / (double) dawnTicks);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    private static double wrap01(double x) {
        x %= 1.0;
        return x < 0 ? x + 1.0 : x;
    }

    private void grantAdvancement(String id) {
        if (world.getServer() == null) return;
        CommandManager commands = world.getServer().getCommandManager();
        commands.executeWithPrefix(world.getServer().getCommandSource().withSilent(),
                "advancement grant @a only binary_skies:" + id);
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setDebugSpeed(int speed) {
        this.debugSpeed = Math.max(1, Math.min(8, speed));
    }

    public void jumpToPhase(PlanetPhase phase) {
        switch (phase) {
            case DAY -> elapsedTicks = dayTicks / 2;
            case SUNSET -> elapsedTicks = dayTicks + sunsetTicks / 2;
            case NIGHT -> elapsedTicks = dayTicks + sunsetTicks + nightTicks / 2;
            case DAWN -> elapsedTicks = dayTicks + sunsetTicks + nightTicks + dawnTicks / 2;
        }
        updatePhase();
        world.setTimeOfDay((long) Math.floor(dayTimeForProgress()));
    }

    public Text infoText() {
        double progress = elapsedTicks / (double) Math.max(cycleTicks, 1);
        long remaining = Math.max(0, cycleTicks - elapsedTicks);
        return Text.literal(String.format(Locale.ROOT,
                "Phase=%s | cycle=%ds | progress=%.1f%% | remaining=%ds | speed=x%d | anomaly=%s",
                phase.displayName, cycleTicks / 20, progress * 100.0, remaining / 20, debugSpeed,
                BinarySkiesConfig.get().anomaliesEnabled ? "on" : "off"));
    }

    public boolean isNight() {
        return phase == PlanetPhase.NIGHT;
    }
}
