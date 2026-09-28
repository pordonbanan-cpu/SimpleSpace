package com.simplespace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.simplespace.dimension.ModDimensions;
import com.simplespace.event.SpacePlanetSpawner;
import com.simplespace.event.SpaceTransitionHandler;
import com.simplespace.space.VirtualOrbitSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SpaceCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("space")
            .requires(s -> s.hasPermission(0))
            .executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                ServerLevel space = p.server.getLevel(ModDimensions.SPACE_LEVEL);
                if (space == null) {
                    ctx.getSource().sendFailure(Component.literal("Космос не найден"));
                    return 0;
                }
                p.teleportTo(space,
                        SpaceTransitionHandler.SPACE_SPAWN_X,
                        SpaceTransitionHandler.SPACE_SPAWN_Y,
                        SpaceTransitionHandler.SPACE_SPAWN_Z,
                        180f, 5f);
                SpacePlanetSpawner.ensurePlanets(space);
                p.setNoGravity(true);
                VirtualOrbitSystem.resetToEarthOrbit();
                ctx.getSource().sendSuccess(() -> Component.literal("§bКосмос (орбита Земли, кубы)"), false);
                return 1;
            }));

        dispatcher.register(Commands.literal("overworld")
            .requires(s -> s.hasPermission(0))
            .executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
                if (ow == null) return 0;
                p.setNoGravity(false);
                p.teleportTo(ow, p.getX(), 120, p.getZ(), p.getYRot(), p.getXRot());
                ctx.getSource().sendSuccess(() -> Component.literal("§aОбычный мир"), false);
                return 1;
            }));

        dispatcher.register(Commands.literal("moon")
            .requires(s -> s.hasPermission(0))
            .executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                ServerLevel moon = p.server.getLevel(ModDimensions.MOON_LEVEL);
                if (moon == null) {
                    ctx.getSource().sendFailure(Component.literal("Луна не найдена"));
                    return 0;
                }
                p.setNoGravity(false);
                p.teleportTo(moon, 0, 80, 0, p.getYRot(), p.getXRot());
                ctx.getSource().sendSuccess(() -> Component.literal("§7Луна"), false);
                return 1;
            }));

        dispatcher.register(Commands.literal("spawnplanets")
            .requires(s -> s.hasPermission(0))
            .executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                if (p.level() instanceof ServerLevel level
                        && level.dimension() == ModDimensions.SPACE_LEVEL) {
                    SpacePlanetSpawner.forceRespawn(level);
                    ctx.getSource().sendSuccess(() -> Component.literal("§eКубические планеты пересозданы"), false);
                    return 1;
                }
                ctx.getSource().sendFailure(Component.literal("Нужно быть в космосе"));
                return 0;
            }));

        dispatcher.register(Commands.literal("orbit")
            .requires(s -> s.hasPermission(0))
            .executes(ctx -> {
                Vec3 p = VirtualOrbitSystem.playerVirtualPos;
                double spd = VirtualOrbitSystem.speed();
                ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                        "§bОрбита: pos (%.0f, %.0f, %.0f)  v=%.1f", p.x, p.y, p.z, spd)), false);
                return 1;
            })
            .then(Commands.literal("prograde")
                .executes(ctx -> {
                    Vec3 v = VirtualOrbitSystem.playerVirtualVel;
                    if (v.lengthSqr() < 1e-6) v = new Vec3(0, 0, 1);
                    VirtualOrbitSystem.applyImpulse(v.normalize().scale(5.0));
                    ctx.getSource().sendSuccess(() -> Component.literal("§a+5 Δv prograde"), false);
                    return 1;
                }))
            .then(Commands.literal("retrograde")
                .executes(ctx -> {
                    Vec3 v = VirtualOrbitSystem.playerVirtualVel;
                    if (v.lengthSqr() < 1e-6) v = new Vec3(0, 0, 1);
                    VirtualOrbitSystem.applyImpulse(v.normalize().scale(-5.0));
                    ctx.getSource().sendSuccess(() -> Component.literal("§e-5 Δv retrograde"), false);
                    return 1;
                }))
        );
    }
}
