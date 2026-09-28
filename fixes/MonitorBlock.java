package com.simplespace.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Монитор: тонкая панель, стыкуется с соседями (2×2, 3×3, 5×5…).
 * up/down/left/right = true если рядом такой же монитор с тем же facing.
 */
public class MonitorBlock extends Block {

    public static final MapCodec<MonitorBlock> CODEC = simpleCodec(MonitorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    private static final VoxelShape NORTH = Block.box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 3, 16, 16);
    private static final VoxelShape WEST = Block.box(13, 0, 0, 16, 16, 16);

    public MonitorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, UP, DOWN, LEFT, RIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return connect(s, ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connect(state, level, pos);
    }

    private BlockState connect(BlockState state, LevelAccessor level, BlockPos pos) {
        Direction f = state.getValue(FACING);
        return state
                .setValue(UP, joins(level, pos.above(), f))
                .setValue(DOWN, joins(level, pos.below(), f))
                .setValue(LEFT, joins(level, pos.relative(f.getClockWise()), f))
                .setValue(RIGHT, joins(level, pos.relative(f.getCounterClockWise()), f));
    }

    private boolean joins(LevelAccessor level, BlockPos p, Direction facing) {
        BlockState s = level.getBlockState(p);
        return s.is(this) && s.getValue(FACING) == facing;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }
}
