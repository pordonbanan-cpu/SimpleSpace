package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class TarsMineGoal extends Goal {

    private final TarsEntity tars;
    private BlockPos target;
    private int digCooldown;
    private int stuckTicks;

    public TarsMineGoal(TarsEntity tars) {
        this.tars = tars;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (tars.getFlashLevel() < 2) return false;
        if (!tars.isMiningOrdered()) return false;
        target = tars.findNearestOre(tars.getMineOreFilter());
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isMiningOrdered() || tars.getFlashLevel() < 2) return false;
        if (target == null) return false;
        if (tars.level().getBlockState(target).isAir()) {
            target = tars.findNearestOre(tars.getMineOreFilter());
            return target != null;
        }
        return true;
    }

    @Override
    public void start() {
        digCooldown = 0;
        stuckTicks = 0;
        tars.setDigging(true);
    }

    @Override
    public void stop() {
        tars.setDigging(false);
        tars.getNavigation().stop();
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) return;

        double dist = tars.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        tars.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        if (dist > 2.8) {
            if (tars.tickCount % 8 == 0) {
                Path path = tars.getNavigation().createPath(target, 1);
                if (path != null) {
                    tars.getNavigation().moveTo(path, tars.getFlashLevel() >= 3 ? 1.35 : 1.15);
                    stuckTicks = 0;
                } else {
                    stuckTicks++;
                }
            }
            if (stuckTicks > 15 || dist < 25) {
                digToward(target);
            }
        } else {
            tars.getNavigation().stop();
            if (--digCooldown <= 0) {
                digCooldown = tars.getFlashLevel() >= 3 ? 8 : 12;
                if (!tars.level().getBlockState(target).isAir()) {
                    tars.level().destroyBlock(target, true, tars);
                    tars.level().playSound(null, target, SoundEvents.STONE_BREAK,
                            SoundSource.BLOCKS, 0.7f, 1.0f);
                    tars.setDigging(true);
                    ServerPlayer sp = owner();
                    if (sp != null && tars.getRandom().nextInt(4) == 0) {
                        tars.speak(sp, "Руда добыта.");
                    }
                }
                target = tars.findNearestOre(tars.getMineOreFilter());
                if (target == null) {
                    tars.setMiningOrdered(false);
                    ServerPlayer sp = owner();
                    if (sp != null) tars.speak(sp, "Больше руды рядом нет.");
                }
            }
        }
    }

    private void digToward(BlockPos goal) {
        if (--digCooldown > 0) return;
        digCooldown = tars.getFlashLevel() >= 3 ? 6 : 10;

        BlockPos base = tars.blockPosition();
        int dx = Integer.signum(goal.getX() - base.getX());
        int dz = Integer.signum(goal.getZ() - base.getZ());
        int dy = Integer.signum(goal.getY() - base.getY());

        BlockPos[] candidates = new BlockPos[] {
                base.offset(dx, 0, dz),
                base.offset(dx, 1, dz),
                base.offset(dx, dy, dz),
                base.above(),
                base.offset(0, dy, 0)
        };
        for (BlockPos p : candidates) {
            BlockState st = tars.level().getBlockState(p);
            if (st.isAir() || st.getDestroySpeed(tars.level(), p) < 0) continue;
            if (st.is(Blocks.BEDROCK) || st.is(Blocks.BARRIER)) continue;
            tars.level().destroyBlock(p, true, tars);
            tars.level().playSound(null, p, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5f, 0.9f);
            tars.setDigging(true);
            stuckTicks = 0;
            return;
        }
    }

    private ServerPlayer owner() {
        var o = tars.getOwner();
        return o instanceof ServerPlayer sp ? sp : null;
    }
}
