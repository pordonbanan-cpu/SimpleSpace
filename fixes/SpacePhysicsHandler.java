package com.simplespace.event;

import com.simplespace.dimension.ModDimensions;
import com.simplespace.entity.CelestialBodyEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class SpacePhysicsHandler {

    private static final double ENTER_FACTOR = 0.55;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        Level level = player.level();

        if (level.dimension() == ModDimensions.SPACE_LEVEL) {
            player.setNoGravity(true);

            if (!(level instanceof ServerLevel space)) return;

            for (Entity e : space.getAllEntities()) {
                if (!(e instanceof CelestialBodyEntity body)) continue;
                double radius = body.getBbWidth() * 0.5 * ENTER_FACTOR;
                double dist = player.position().distanceTo(body.position());
                if (dist < radius) {
                    enterPlanet(player, body);
                    return;
                }
            }
        } else {
            if (player.isNoGravity() && level.dimension() != ModDimensions.SPACE_LEVEL) {
                player.setNoGravity(false);
            }
        }
    }

    private void enterPlanet(ServerPlayer player, CelestialBodyEntity body) {
        String name = body.getType().toShortString();
        ServerLevel target = null;
        double spawnX = 0, spawnY = 120, spawnZ = 0;

        if (name.contains("earth")) {
            target = player.server.getLevel(Level.OVERWORLD);
            spawnY = SpaceTransitionHandler.SPACE_HEIGHT - 80;
        } else if (name.contains("moon")) {
            target = player.server.getLevel(ModDimensions.MOON_LEVEL);
            spawnY = 80;
        } else if (name.contains("sun")) {
            Vec3 away = player.position().subtract(body.position()).normalize().scale(body.getBbWidth());
            player.teleportTo(player.getX() + away.x, player.getY() + away.y, player.getZ() + away.z);
            return;
        }

        if (target != null) {
            player.setNoGravity(false);
            player.teleportTo(target, spawnX, spawnY, spawnZ, player.getYRot(), player.getXRot());
        }
    }
}
