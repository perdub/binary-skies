package com.alisk.binaryskies;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public final class SecondStarRenderer {
    private static final double TWO_PI = Math.PI * 2.0;

    public void render(ServerWorld world, double phase) {
        BinarySkiesConfig config = BinarySkiesConfig.get();
        if (!config.secondStarEnabled) return;

        double theta = phase * TWO_PI;
        double altitude = Math.sin(theta) * Math.toRadians(config.secondStarMaxAltitudeDegrees);
        double horizonFactor = Math.sin(theta);
        if (horizonFactor <= -0.05) return;

        double azimuth = theta + Math.toRadians(config.secondStarYawOffsetDegrees);
        double horizontal = Math.cos(altitude);
        Vec3d direction = new Vec3d(
                Math.cos(azimuth) * horizontal,
                Math.sin(altitude),
                Math.sin(azimuth) * horizontal
        ).normalize();

        Vector3f color = new Vector3f(
                ((config.secondStarColor >> 16) & 0xFF) / 255.0f,
                ((config.secondStarColor >> 8) & 0xFF) / 255.0f,
                (config.secondStarColor & 0xFF) / 255.0f
        );

        float scale = config.secondStarParticleScale * (float) Math.max(0.45, Math.min(1.0, horizonFactor + 0.15));
        DustParticleEffect dust = new DustParticleEffect(color, scale);

        for (ServerPlayerEntity player : world.getPlayers()) {
            Vec3d center = player.getEyePos().add(direction.multiply(config.secondStarDistance));
            world.spawnParticles(player, ParticleTypes.END_ROD, true, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            spawn(world, player, dust, center);
            spawn(world, player, dust, center.add(0.45, 0, 0));
            spawn(world, player, dust, center.add(-0.45, 0, 0));
            spawn(world, player, dust, center.add(0, 0.45, 0));
            spawn(world, player, dust, center.add(0, -0.45, 0));
        }
    }

    private void spawn(ServerWorld world, ServerPlayerEntity player, DustParticleEffect dust, Vec3d pos) {
        world.spawnParticles(player, dust, true, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }
}
