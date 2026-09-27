package com.alisk.binaryskies;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public final class BinarySkies implements ModInitializer {
    public static final String MOD_ID = "binary_skies";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<RegistryKey<World>, PlanetCycle> CYCLES = new HashMap<>();
    private static MinecraftServer server;

    @Override
    public void onInitialize() {
        BinarySkiesConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(started -> {
            server = started;
            CYCLES.clear();
            for (ServerWorld world : started.getWorlds()) {
                if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                    CYCLES.put(world.getRegistryKey(), new PlanetCycle(world));
                }
            }
            LOGGER.info("Binary Skies initialized: {}", BinarySkiesConfig.get());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(stopping -> {
            CYCLES.clear();
            server = null;
        });

        ServerTickEvents.END_SERVER_TICK.register(BinarySkies::tickServer);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    private static void tickServer(MinecraftServer minecraftServer) {
        if (server != minecraftServer) {
            server = minecraftServer;
        }

        ServerWorld overworld = minecraftServer.getOverworld();
        RegistryKey<World> key = overworld.getRegistryKey();
        PlanetCycle cycle = CYCLES.computeIfAbsent(key, ignored -> new PlanetCycle(overworld));
        cycle.tick();
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("skies")
                .requires(source -> source.hasPermissionLevel(2));

        root.then(CommandManager.literal("info").executes(context -> {
            PlanetCycle cycle = getCycle(context.getSource().getWorld());
            context.getSource().sendFeedback(() -> cycle.infoText(), false);
            return 1;
        }));

        root.then(CommandManager.literal("pause").executes(context -> {
            PlanetCycle cycle = getCycle(context.getSource().getWorld());
            cycle.setPaused(true);
            context.getSource().sendFeedback(() -> Text.literal("Binary Skies paused."), true);
            return 1;
        }));

        root.then(CommandManager.literal("resume").executes(context -> {
            PlanetCycle cycle = getCycle(context.getSource().getWorld());
            cycle.setPaused(false);
            context.getSource().sendFeedback(() -> Text.literal("Binary Skies resumed."), true);
            return 1;
        }));

        root.then(CommandManager.literal("reset").executes(context -> {
            PlanetCycle cycle = getCycle(context.getSource().getWorld());
            cycle.reset();
            context.getSource().sendFeedback(() -> Text.literal("Binary Skies cycle reset."), true);
            return 1;
        }));

        root.then(CommandManager.literal("setphase")
                .then(CommandManager.argument("phase", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (PlanetPhase phase : PlanetPhase.values()) builder.suggest(phase.name().toLowerCase());
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "phase");
                            try {
                                PlanetPhase phase = PlanetPhase.valueOf(raw.toUpperCase());
                                getCycle(context.getSource().getWorld()).jumpToPhase(phase);
                                context.getSource().sendFeedback(() -> Text.literal("Phase set to " + phase.displayName + "."), true);
                                return 1;
                            } catch (IllegalArgumentException ex) {
                                context.getSource().sendError(Text.literal("Unknown phase: " + raw));
                                return 0;
                            }
                        })));

        root.then(CommandManager.literal("speed")
                .then(CommandManager.argument("multiplier", IntegerArgumentType.integer(1, 8))
                        .executes(context -> {
                            int multiplier = IntegerArgumentType.getInteger(context, "multiplier");
                            getCycle(context.getSource().getWorld()).setDebugSpeed(multiplier);
                            context.getSource().sendFeedback(() -> Text.literal("Debug speed set to x" + multiplier + "."), true);
                            return 1;
                        })));

        root.then(CommandManager.literal("anomalies")
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            BinarySkiesConfig.get().anomaliesEnabled = BoolArgumentType.getBool(context, "enabled");
                            BinarySkiesConfig.save();
                            context.getSource().sendFeedback(() -> Text.literal("Anomalies: " + BinarySkiesConfig.get().anomaliesEnabled), true);
                            return 1;
                        })));

        dispatcher.register(root);
    }

    private static PlanetCycle getCycle(ServerWorld world) {
        if (world.getRegistryKey() != net.minecraft.world.World.OVERWORLD) {
            return CYCLES.values().stream().findFirst().orElseGet(() -> new PlanetCycle(world));
        }
        RegistryKey<World> key = world.getRegistryKey();
        return CYCLES.computeIfAbsent(key, ignored -> new PlanetCycle(world));
    }

    public static MinecraftServer getServer() {
        return server;
    }
}
