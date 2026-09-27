package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

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
    private int detourIdx;
    private double lastDist = Double.MAX_VALUE;

    private static final int[][] DETOURS = {
            {0, 0}, {3, 0}, {-3, 0}, {0, 3}, {0, -3},
            {4, 4}, {4, -4}, {-4, 4}, {-4, -4}, {6, 0}, {-6, 0}
    };

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
        detourIdx = 0;
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
        if (dist < lastDist - 0.15) {
            noProgress = 0;
            lastDist = dist;
        } else {
            noProgress++;
        }

        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = tars.isSprintMode() ? 4 : 6;
            double spd = tars.isSprintMode() ? speed * 2.0 : (dist > 64 ? speed * 1.45 : speed);

            boolean moved = tryMove(owner.blockPosition(), spd);
            if (!moved && noProgress > 8) {
                for (int i = 0; i < DETOURS.length; i++) {
                    int idx = (detourIdx + i) % DETOURS.length;
                    int[] d = DETOURS[idx];
                    BlockPos side = owner.blockPosition().offset(d[0], 0, d[1]);
                    if (tryMove(side, spd)) {
                        detourIdx = (idx + 1) % DETOURS.length;
                        moved = true;
                        break;
                    }
                }
            }
            if (moved) tars.setDigging(false);
        }

        boolean navStuck = tars.getNavigation().isDone() && dist > 9;
        if (noProgress > 35 || (navStuck && noProgress > 20)) {
            digToward(BlockPos.containing(owner.getX(), owner.getY(), owner.getZ()));
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
        int dx = Integer.signum(goal.getX() - base.getX());
        int dy = Integer.signum(goal.getY() - base.getY());
        int dz = Integer.signum(goal.getZ() - base.getZ());

        BlockPos[] candidates = new BlockPos[] {
                base.offset(dx, 1, dz),
                base.offset(dx, 0, dz),
                base.offset(dx, dy, dz),
                base.offset(0, 1, 0),
                base.offset(dx, -1, dz),
                base.offset(dx, 0, 0),
                base.offset(0, 0, dz)
        };
        for (BlockPos p : candidates) {
            BlockState st = tars.level().getBlockState(p);
            if (st.isAir()) continue;
            if (st.getDestroySpeed(tars.level(), p) < 0) continue;
            if (st.is(Blocks.BEDROCK) || st.is(Blocks.BARRIER) || st.is(Blocks.OBSIDIAN)) continue;
            tars.breakBlockForMine(p);
            noProgress = Math.max(0, noProgress - 10);
            lastDist = Double.MAX_VALUE;
            return;
        }
    }
}
