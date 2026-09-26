package com.simplespace.tars;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class TarsFollowOwnerGoal extends Goal {

    private final TarsEntity tars;
    private final double speed;
    private final float stopDistance;
    private final float startDistance;
    private Player owner;
    private int timeToRecalcPath;

    public TarsFollowOwnerGoal(TarsEntity tars, double speed, float stopDistance, float startDistance) {
        this.tars = tars;
        this.speed = speed;
        this.stopDistance = stopDistance;
        this.startDistance = startDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!tars.isFollowing()) return false;
        Player o = tars.getOwner();
        if (o == null || o.isSpectator() || !o.isAlive()) return false;
        this.owner = o;
        return tars.distanceToSqr(o) > (double) (startDistance * startDistance);
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isFollowing() || owner == null || !owner.isAlive()) return false;
        return tars.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
    }

    @Override
    public void start() {
        timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        owner = null;
        tars.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        tars.getLookControl().setLookAt(owner, 25.0f, tars.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 5;
            double distSq = tars.distanceToSqr(owner);
            double spd = speed;
            if (distSq > 64) spd = speed * 1.35;
            tars.getNavigation().moveTo(owner, spd);
        }
    }
}
