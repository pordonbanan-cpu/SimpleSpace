package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class TarsGoToGoal extends Goal {

    private final TarsEntity tars;
    private final double speed;
    private int recalc;
    private int noProgress;
    private int digCd;
    private int detourIdx;
    private double lastDist = Double.MAX_VALUE;

    private static final int[][] DETOURS = {
            {0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3},
            {4, 4}, {4, -4}, {-4, 4}, {-4, -4}
    };

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
        detourIdx = 0;
        lastDist = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
        tars.clearGoToTarget(true);
        tars.getNavigation().stop();
        tars.setDigging(false);
        tars.setClimbing(false);
    }

    @Override
    public void tick() {
        BlockPos t = tars.getGoToTarget();
        if (t == null) return;

        double dist = tars.distanceToSqr(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
        if (dist < lastDist - 0.15) {
            noProgress = 0;
            lastDist = dist;
        } else {
            noProgress++;
        }

        if (--recalc <= 0) {
            recalc = 8;
            double spd = tars.isSprintMode() ? speed * 2.0 : speed;
            boolean moved = tryMove(t, spd);
            if (!moved && noProgress > 8) {
                for (int i = 0; i < DETOURS.length; i++) {
                    int idx = (detourIdx + i) % DETOURS.length;
                    int[] d = DETOURS[idx];
                    if (tryMove(t.offset(d[0], 0, d[1]), spd)) {
                        detourIdx = (idx + 1) % DETOURS.length;
                        moved = true;
                        break;
                    }
                }
            }
            if (moved) {
                tars.setDigging(false);
                tars.setClimbing(false);
            }
        }

        boolean navStuck = tars.getNavigation().isDone() && dist > 4;
        if (noProgress > 30 || (navStuck && noProgress > 18)) {
            digToward(t);
        }
    }

    private boolean tryMove(BlockPos dest, double spd) {
        Path path = tars.getNavigation().createPath(dest, 1);
        if (path == null || path.getNodeCount() == 0) return false;
        return tars.getNavigation().moveTo(path, spd);
    }

    private void digToward(BlockPos goal) {
        if (--digCd > 0) return;
        digCd = 7;
        tars.setDigging(true);

        BlockPos base = tars.blockPosition();
        BlockPos under = base.below();
        int dx = Integer.signum(goal.getX() - base.getX());
        int dy = Integer.signum(goal.getY() - base.getY());
        int dz = Integer.signum(goal.getZ() - base.getZ());
        boolean needDown = goal.getY() < base.getY() - 1;

        BlockPos[] candidates = new BlockPos[] {
                base.offset(dx, 1, dz),
                base.offset(0, 1, 0),
                base.offset(dx, 0, dz),
                base.offset(dx, dy > 0 ? 1 : 0, dz)
        };
        for (BlockPos p : candidates) {
            if (p.equals(base) || (!needDown && p.equals(under))) continue;
            if (p.getY() < under.getY() && !needDown) continue;
            BlockState st = tars.level().getBlockState(p);
            if (st.isAir()) continue;
            if (st.getDestroySpeed(tars.level(), p) < 0) continue;
            if (st.is(Blocks.BEDROCK) || st.is(Blocks.BARRIER) || st.is(Blocks.OBSIDIAN)) continue;
            tars.setClimbing(p.getY() > base.getY());
            tars.breakBlockForMine(p);
            noProgress = Math.max(0, noProgress - 10);
            lastDist = Double.MAX_VALUE;
            return;
        }
    }
}
