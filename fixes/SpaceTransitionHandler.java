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
    public static final int RETURN_HEIGHT = -20;

    public static final double SPACE_SPAWN_X = 0.0;
    public static final double SPACE_SPAWN_Y = 10.0;
    public static final double SPACE_SPAWN_Z = 55.0;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        Level level = player.level();

        if (level.dimension() == Level.OVERWORLD) {
            if (player.getY() >= SPACE_HEIGHT) {
                ServerLevel spaceLevel = player.server.getLevel(ModDimensions.SPACE_LEVEL);
                if (spaceLevel != null) {
                    player.teleportTo(spaceLevel, SPACE_SPAWN_X, SPACE_SPAWN_Y, SPACE_SPAWN_Z, 180f, 5f);
                    SpacePlanetSpawner.ensurePlanets(spaceLevel);
                    player.setNoGravity(true);
                    SimpleSpace.LOGGER.info("Player {} entered space beside Earth", player.getName().getString());
                }
            }
        }
    }
}
