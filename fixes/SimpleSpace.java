package com.simplespace;

import com.simplespace.block.ModBlocks;
import com.simplespace.block.entity.ModBlockEntities;
import com.simplespace.command.TarsCommands;
import com.simplespace.entity.ModEntities;
import com.simplespace.item.ModItems;
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
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::entityAttributes);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);

        LOGGER.info("SimpleSpace + TARS loaded (no space dimension)");
    }

    private void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TARS.get(), TarsEntity.createAttributes().build());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        TarsCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("SimpleSpace common setup complete");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("SimpleSpace client setup complete");
    }
}
