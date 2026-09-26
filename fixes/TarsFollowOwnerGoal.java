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
        float start = tars.isSprintMode() ? 1.2f : startDistance;
        return tars.distanceToSqr(o) > (double) (start * start);
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isFollowing() || owner == null || !owner.isAlive()) return false;
        float stop = tars.isSprintMode() ? 1.2f : stopDistance;
        return tars.distanceToSqr(owner) > (double) (stop * stop);
    }

    @Override
    public void start() { timeToRecalcPath = 0; }

    @Override
    public void stop() {
        owner = null;
        tars.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        tars.getLookControl().setLookAt(owner, 30.0f, tars.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = tars.isSprintMode() ? 3 : 5;
            double spd = speed;
            if (tars.isSprintMode()) spd = speed * 2.0;
            else if (tars.distanceToSqr(owner) > 64) spd = speed * 1.45;
            tars.getNavigation().moveTo(owner, spd);
        }
    }
}
