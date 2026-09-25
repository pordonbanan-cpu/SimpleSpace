package com.simplespace.event;

import com.simplespace.SimpleSpace;
import com.simplespace.dimension.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class SpaceTransitionHandler {

    public static final int SPACE_HEIGHT = 10000;
    public static final int RETURN_HEIGHT = 0;

    // Above Earth
    public static final double SPACE_SPAWN_X = 0.0;
    public static final double SPACE_SPAWN_Y = 130.0;
    public static final double SPACE_SPAWN_Z = 90.0;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        Level level = player.level();

        if (level.dimension() == Level.OVERWORLD) {
            if (player.getY() >= SPACE_HEIGHT) {
                ServerLevel spaceLevel = player.server.getLevel(ModDimensions.SPACE_LEVEL);
                if (spaceLevel != null) {
                    player.teleportTo(spaceLevel, SPACE_SPAWN_X, SPACE_SPAWN_Y, SPACE_SPAWN_Z, 180f, 25f);
                    SpacePlanetSpawner.ensurePlanets(spaceLevel);
                    SimpleSpace.LOGGER.info("Player {} entered space", player.getName().getString());
                }
            }
        } else if (level.dimension() == ModDimensions.SPACE_LEVEL) {
            if (player.getY() < RETURN_HEIGHT) {
                ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    player.teleportTo(overworld, player.getX(), SPACE_HEIGHT - 50, player.getZ(),
                            player.getYRot(), player.getXRot());
                    SimpleSpace.LOGGER.info("Player {} returned from space", player.getName().getString());
                }
            }
        }
    }
}
