package com.simplespace.block;

import com.simplespace.SimpleSpace;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SimpleSpace.MOD_ID);

    public static final DeferredBlock<Block> TARS_DOCK = BLOCKS.register("tars_dock",
            () -> new TarsDockBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 8f)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<Block> SPACE_SCANNER = BLOCKS.register("space_scanner",
            () -> new SpaceScannerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(2.5f, 6f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 7)));

    public static final DeferredBlock<Block> GROUND_COMM = BLOCKS.register("ground_comm",
            () -> new GroundCommBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.5f, 6f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 5)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
