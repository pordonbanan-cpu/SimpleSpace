package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class TarsFollowOwnerGoal extends Goal {

    private final TarsEntity tars;
    private final double speed;
    private final float stopDistance;
    private final float startDistance;
    private Player owner;
    private int timeToRecalcPath;
    private int noProgress;
    private int digCd;
    private double lastDist = Double.MAX_VALUE;

    public TarsFollowOwnerGoal(TarsEntity tars, double speed, float stopDistance, float startDistance) {
        this.tars = tars;
        this.speed = speed;
        this.stopDistance = stopDistance;
        this.startDistance = startDistance;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!tars.isFollowing()) return false;
        if (tars.getGoToTarget() != null) return false;
        if (tars.isMiningOrdered()) return false;
        Player o = tars.getOwner();
        if (o == null || o.isSpectator() || !o.isAlive()) return false;
        this.owner = o;
        float start = tars.isSprintMode() ? 1.2f : startDistance;
        return tars.distanceToSqr(o) > (double) (start * start);
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isFollowing() || tars.getGoToTarget() != null || tars.isMiningOrdered()) return false;
        if (owner == null || !owner.isAlive()) return false;
        float stop = tars.isSprintMode() ? 1.2f : stopDistance;
        return tars.distanceToSqr(owner) > (double) (stop * stop);
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
        noProgress = 0;
        digCd = 0;
        lastDist = Double.MAX_VALUE;
    }

    @Override
    public void stop() {
        owner = null;
        tars.getNavigation().stop();
        tars.setDigging(false);
    }

    @Override
    public void tick() {
        if (owner == null) return;

        double dist = tars.distanceToSqr(owner);
        if (dist < lastDist - 0.1) {
            noProgress = 0;
            lastDist = dist;
        } else {
            noProgress++;
        }

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = tars.isSprintMode() ? 4 : 6;
            double spd = speed;
            if (tars.isSprintMode()) spd = speed * 2.0;
            else if (dist > 64) spd = speed * 1.45;
            tars.getNavigation().moveTo(owner, spd);
        }

        boolean navStuck = tars.getNavigation().isDone() && dist > 9;
        if (noProgress > 15 || navStuck) {
            digToward(BlockPos.containing(owner.getX(), owner.getY(), owner.getZ()));
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
