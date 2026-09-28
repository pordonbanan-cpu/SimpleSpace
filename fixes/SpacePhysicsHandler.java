package com.simplespace.event;

import com.simplespace.dimension.ModDimensions;
import com.simplespace.entity.CelestialBodyEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class SpacePhysicsHandler {

    /** Доля половины BB: 0.75 ≈ войти в куб глубже середины грани. */
    private static final double ENTER_FACTOR = 0.75;
    private static int cooldown;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        Level level = player.level();

        if (level.dimension() == ModDimensions.SPACE_LEVEL) {
            player.setNoGravity(true);
            if (cooldown > 0) {
                cooldown--;
                return;
            }

            if (!(level instanceof ServerLevel space)) return;

            for (Entity e : space.getAllEntities()) {
                if (!(e instanceof CelestialBodyEntity body)) continue;
                double half = body.getBbWidth() * 0.5;
                double radius = half * ENTER_FACTOR;
                double dist = player.position().distanceTo(body.position());
                if (dist < radius) {
                    enterPlanet(player, body);
                    cooldown = 40;
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
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(body.getType());
        String path = key != null ? key.getPath() : body.getType().toShortString();

        ServerLevel target = null;
        double spawnX = player.getX();
        double spawnY = 120;
        double spawnZ = player.getZ();
        String msg = null;

        if (path.contains("earth")) {
            target = player.server.getLevel(Level.OVERWORLD);
            spawnX = 0;
            spawnY = Math.max(80, SpaceTransitionHandler.SPACE_HEIGHT - 80);
            spawnZ = 0;
            msg = "§aВход в атмосферу Земли";
        } else if (path.contains("moon")) {
            target = player.server.getLevel(ModDimensions.MOON_LEVEL);
            spawnX = 0;
            spawnY = 80;
            spawnZ = 0;
            msg = "§7Посадка на Луну";
        } else if (path.contains("sun")) {
            Vec3 away = player.position().subtract(body.position());
            if (away.lengthSqr() < 1e-6) away = new Vec3(1, 0, 0);
            away = away.normalize().scale(body.getBbWidth() * 0.7);
            player.teleportTo(player.getX() + away.x, player.getY() + away.y, player.getZ() + away.z);
            player.sendSystemMessage(Component.literal("§cСлишком горячо. Отлет от Солнца."));
            return;
        }

        if (target != null) {
            player.setNoGravity(false);
            player.teleportTo(target, spawnX, spawnY, spawnZ, player.getYRot(), player.getXRot());
            if (msg != null) player.sendSystemMessage(Component.literal(msg));
        }
    }
}
