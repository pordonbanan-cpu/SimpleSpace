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
