package com.simplespace;

import com.simplespace.command.SpaceCommands;
import com.simplespace.command.TarsCommands;
import com.simplespace.entity.ModEntities;
import com.simplespace.event.SpacePhysicsHandler;
import com.simplespace.event.SpacePlanetSpawner;
import com.simplespace.event.SpaceTransitionHandler;
import com.simplespace.tars.TarsEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SimpleSpace.MOD_ID)
public class SimpleSpace {
    public static final String MOD_ID = "simplespace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleSpace(IEventBus modEventBus) {
        ModEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::entityAttributes);

        NeoForge.EVENT_BUS.register(new SpaceTransitionHandler());
        NeoForge.EVENT_BUS.register(new SpacePlanetSpawner());
        NeoForge.EVENT_BUS.register(new SpacePhysicsHandler());

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("SimpleSpace + TARS loaded");
    }

    private void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TARS.get(), TarsEntity.createAttributes().build());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        SpaceCommands.register(event.getDispatcher());
        TarsCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("SimpleSpace common setup complete");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("SimpleSpace client setup complete");
    }
}
