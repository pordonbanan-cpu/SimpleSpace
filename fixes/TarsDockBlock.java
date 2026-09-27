package com.simplespace.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт-колыбель под TARS (~2 блока высотой, ~1 блок ширины).
 * Пол + боковые направляющие + задняя стенка.
 */
public class TarsDockBlock extends Block {

    // floor 16x4x16 + left rail + right rail + back
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape LEFT = Block.box(0, 3, 0, 2, 28, 16);
    private static final VoxelShape RIGHT = Block.box(14, 3, 0, 16, 28, 16);
    private static final VoxelShape BACK = Block.box(2, 3, 14, 14, 24, 16);
    private static final VoxelShape SHAPE = Shapes.or(FLOOR, LEFT, RIGHT, BACK);

    public TarsDockBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return FLOOR;
    }
}
