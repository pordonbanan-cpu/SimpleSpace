package com.simplespace.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Шлюзовой люк 2×2. Master = bottom_right.
 * Свойства: facing, part, status (closed/open/moving).
 */
public class AirlockDoorBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<AirlockDoorBlock> CODEC = simpleCodec(AirlockDoorBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<Status> STATUS = EnumProperty.create("status", Status.class);

    public enum Part implements StringRepresentable {
        BOTTOM_RIGHT("bottom_right"),
        BOTTOM_LEFT("bottom_left"),
        TOP_RIGHT("top_right"),
        TOP_LEFT("top_left");

        private final String name;
        Part(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    public enum Status implements StringRepresentable {
        CLOSED("closed"), OPEN("open"), MOVING("moving");
        private final String name;
        Status(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }

    // тонкая панель
    private static final VoxelShape NORTH = Block.box(0, 0, 4, 16, 16, 12);
    private static final VoxelShape SOUTH = Block.box(0, 0, 4, 16, 16, 12);
    private static final VoxelShape EAST = Block.box(4, 0, 0, 12, 16, 16);
    private static final VoxelShape WEST = Block.box(4, 0, 0, 12, 16, 16);

    public AirlockDoorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.BOTTOM_RIGHT)
                .setValue(STATUS, Status.CLOSED));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, PART, STATUS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        // нужно место 2x2: right = facing.counterClockWise? master bottom_right
        // left = clockwise from facing when looking at door
        BlockPos left = pos.relative(facing.getClockWise());
        BlockPos up = pos.above();
        BlockPos upLeft = left.above();
        if (!level.getBlockState(left).canBeReplaced(ctx)
                || !level.getBlockState(up).canBeReplaced(ctx)
                || !level.getBlockState(upLeft).canBeReplaced(ctx)
                || pos.getY() > level.getMaxBuildHeight() - 1) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, Part.BOTTOM_RIGHT).setValue(STATUS, Status.CLOSED);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        Direction facing = state.getValue(FACING);
        BlockPos left = pos.relative(facing.getClockWise());
        BlockPos up = pos.above();
        BlockPos upLeft = left.above();
        level.setBlock(left, state.setValue(PART, Part.BOTTOM_LEFT), 3);
        level.setBlock(up, state.setValue(PART, Part.TOP_RIGHT), 3);
        level.setBlock(upLeft, state.setValue(PART, Part.TOP_LEFT), 3);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        toggle(level, pos, state);
        return InteractionResult.CONSUME;
    }

    private void toggle(Level level, BlockPos pos, BlockState state) {
        BlockPos master = findMaster(pos, state);
        if (master == null) return;
        Status cur = state.getValue(STATUS);
        Status next = cur == Status.OPEN ? Status.CLOSED : Status.OPEN;
        Direction facing = state.getValue(FACING);
        setAll(level, master, facing, next);
    }

    private void setAll(Level level, BlockPos master, Direction facing, Status status) {
        BlockPos left = master.relative(facing.getClockWise());
        BlockPos up = master.above();
        BlockPos upLeft = left.above();
        for (BlockPos p : new BlockPos[]{master, left, up, upLeft}) {
            BlockState s = level.getBlockState(p);
            if (s.is(this)) {
                level.setBlock(p, s.setValue(STATUS, status), 3);
            }
        }
    }

    @Nullable
    private BlockPos findMaster(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        Part part = state.getValue(PART);
        return switch (part) {
            case BOTTOM_RIGHT -> pos;
            case BOTTOM_LEFT -> pos.relative(facing.getCounterClockWise());
            case TOP_RIGHT -> pos.below();
            case TOP_LEFT -> pos.below().relative(facing.getCounterClockWise());
        };
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos master = findMaster(pos, state);
            if (master != null) {
                Direction facing = state.getValue(FACING);
                BlockPos left = master.relative(facing.getClockWise());
                BlockPos up = master.above();
                BlockPos upLeft = left.above();
                for (BlockPos p : new BlockPos[]{master, left, up, upLeft}) {
                    if (level.getBlockState(p).is(this)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (state.getValue(STATUS) == Status.OPEN) {
            return Shapes.empty();
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }
}
