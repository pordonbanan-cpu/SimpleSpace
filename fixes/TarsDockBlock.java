package com.simplespace.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт-колыбель. Визуально — борта и задняя стенка;
 * коллизия только пол, чтобы TARS мог зайти внутрь (перелёты и т.д.).
 */
public class TarsDockBlock extends Block {

    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 3, 16);
    /** Полная форма только для outline в руке/мире (не collision). */
    private static final VoxelShape OUTLINE = Shapes.or(
            FLOOR,
            Block.box(0, 3, 0, 2, 28, 16),
            Block.box(14, 3, 0, 16, 28, 16),
            Block.box(2, 3, 14, 14, 24, 16),
            Block.box(2, 3, 0, 14, 5, 2)
    );

    public TarsDockBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Игроки и мобы — только пол; TARS тоже проходит бортами
        return FLOOR;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return FLOOR;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return OUTLINE;
    }
}
