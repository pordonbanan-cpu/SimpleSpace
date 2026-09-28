package com.simplespace.block.entity;

import com.simplespace.SimpleSpace;
import com.simplespace.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SimpleSpace.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpaceScannerBlockEntity>> SPACE_SCANNER =
            BLOCK_ENTITIES.register("space_scanner", () ->
                    BlockEntityType.Builder.of(SpaceScannerBlockEntity::new, ModBlocks.SPACE_SCANNER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GroundCommBlockEntity>> GROUND_COMM =
            BLOCK_ENTITIES.register("ground_comm", () ->
                    BlockEntityType.Builder.of(GroundCommBlockEntity::new, ModBlocks.GROUND_COMM.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
