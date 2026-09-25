package com.simplespace.event;

import com.simplespace.SimpleSpace;
import com.simplespace.dimension.ModDimensions;
import com.simplespace.entity.CelestialBodyEntity;
import com.simplespace.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class SpacePlanetSpawner {

    private static boolean planetsSpawned = false;

    public static void ensurePlanets(ServerLevel spaceLevel) {
        if (planetsSpawned) return;

        spawnPlanet(spaceLevel, ModEntities.SUN.get(), 0, 140, 60);
        spawnPlanet(spaceLevel, ModEntities.EARTH.get(), -50, 90, -40);
        spawnPlanet(spaceLevel, ModEntities.MOON.get(), -65, 85, -55);

        planetsSpawned = true;
        SimpleSpace.LOGGER.info("Spawned celestial bodies near space spawn");
    }

    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getTo() != ModDimensions.SPACE_LEVEL) return;
        if (!(event.getEntity().level() instanceof ServerLevel spaceLevel)) return;
        ensurePlanets(spaceLevel);
    }

    private static void spawnPlanet(ServerLevel level, EntityType<CelestialBodyEntity> type,
                                    double x, double y, double z) {
        CelestialBodyEntity planet = type.create(level);
        if (planet != null) {
            planet.moveTo(x, y, z, 0, 0);
            planet.setNoGravity(true);
            planet.setInvulnerable(true);
            level.addFreshEntity(planet);
        }
    }

    public static void forceRespawn(ServerLevel spaceLevel) {
        planetsSpawned = false;
        ensurePlanets(spaceLevel);
    }
}
