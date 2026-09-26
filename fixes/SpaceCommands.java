package com.simplespace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.simplespace.dimension.ModDimensions;
import com.simplespace.event.SpacePlanetSpawner;
import com.simplespace.event.SpaceTransitionHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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
                ctx.getSource().sendSuccess(() -> Component.literal("§bКосмос (рядом с Землёй)"), false);
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
                    ctx.getSource().sendSuccess(() -> Component.literal("§eПланеты пересозданы"), false);
                    return 1;
                }
                ctx.getSource().sendFailure(Component.literal("Нужно быть в космосе"));
                return 0;
            }));
    }
}
