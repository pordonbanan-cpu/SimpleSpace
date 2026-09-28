package com.simplespace.entity;

import com.simplespace.SimpleSpace;
import com.simplespace.tars.TarsEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SimpleSpace.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CelestialBodyEntity>> SUN =
            ENTITIES.register("sun", () -> EntityType.Builder
                    .<CelestialBodyEntity>of(CelestialBodyEntity::new, MobCategory.MISC)
                    .sized(36.0f, 36.0f)
                    .clientTrackingRange(1024)
                    .updateInterval(5)
                    .fireImmune()
                    .build("sun"));

    public static final DeferredHolder<EntityType<?>, EntityType<CelestialBodyEntity>> EARTH =
            ENTITIES.register("earth", () -> EntityType.Builder
                    .<CelestialBodyEntity>of(CelestialBodyEntity::new, MobCategory.MISC)
                    .sized(28.0f, 28.0f)
                    .clientTrackingRange(1024)
                    .updateInterval(5)
                    .fireImmune()
                    .build("earth"));

    public static final DeferredHolder<EntityType<?>, EntityType<CelestialBodyEntity>> MOON =
            ENTITIES.register("moon", () -> EntityType.Builder
                    .<CelestialBodyEntity>of(CelestialBodyEntity::new, MobCategory.MISC)
                    .sized(10.0f, 10.0f)
                    .clientTrackingRange(1024)
                    .updateInterval(5)
                    .fireImmune()
                    .build("moon"));

    public static final DeferredHolder<EntityType<?>, EntityType<TarsEntity>> TARS =
            ENTITIES.register("tars", () -> EntityType.Builder
                    .<TarsEntity>of(TarsEntity::new, MobCategory.CREATURE)
                    .sized(0.75f, 2.0f)
                    .clientTrackingRange(64)
                    .build("tars"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
