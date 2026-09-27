package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class TarsGoToGoal extends Goal {

    private final TarsEntity tars;
    private final double speed;
    private int recalc;
    private int noProgress;
    private int digCd;
    private double lastDist = Double.MAX_VALUE;

    public TarsGoToGoal(TarsEntity tars, double speed) {
        this.tars = tars;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return tars.getGoToTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos t = tars.getGoToTarget();
        if (t == null) return false;
        return tars.distanceToSqr(t.getX() + 0.5, t.getY(), t.getZ() + 0.5) > 2.25;
    }

    @Override
    public void start() {
        recalc = 0;
        noProgress = 0;
        digCd = 0;
        lastDist = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
        tars.clearGoToTarget(true);
        tars.getNavigation().stop();
        tars.setDigging(false);
    }

    @Override
    public void tick() {
        BlockPos t = tars.getGoToTarget();
        if (t == null) return;

        double dist = tars.distanceToSqr(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
        if (dist < lastDist - 0.1) {
            noProgress = 0;
            lastDist = dist;
        } else {
            noProgress++;
        }

        if (--recalc <= 0) {
            recalc = 8;
            double spd = tars.isSprintMode() ? speed * 2.0 : speed;
            tars.getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, spd);
        }

        boolean navStuck = tars.getNavigation().isDone() && dist > 4;
        if (noProgress > 12 || navStuck) {
            digToward(t);
        } else {
            tars.setDigging(false);
        }
    }

    private void digToward(BlockPos goal) {
        if (--digCd > 0) return;
        digCd = 6;
        tars.setDigging(true);

        BlockPos base = tars.blockPosition();
        int dx = Integer.signum(goal.getX() - base.getX());
        int dy = Integer.signum(goal.getY() - base.getY());
        int dz = Integer.signum(goal.getZ() - base.getZ());

        BlockPos[] candidates = new BlockPos[] {
                base.offset(dx, 0, dz),
                base.offset(dx, 1, dz),
                base.offset(dx, dy, dz),
                base.offset(0, 1, 0),
                base.offset(dx, -1, dz),
                base.above(),
                base.offset(dx, 0, 0),
                base.offset(0, 0, dz)
        };
        for (BlockPos p : candidates) {
            BlockState st = tars.level().getBlockState(p);
            if (st.isAir()) continue;
            if (st.getDestroySpeed(tars.level(), p) < 0) continue;
            if (st.is(Blocks.BEDROCK) || st.is(Blocks.BARRIER) || st.is(Blocks.OBSIDIAN)) continue;
            tars.breakBlockForMine(p);
            noProgress = 0;
            lastDist = Double.MAX_VALUE;
            return;
        }
    }
}
