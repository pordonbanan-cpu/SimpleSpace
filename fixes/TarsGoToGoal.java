package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class TarsGoToGoal extends Goal {

    private final TarsEntity tars;
    private final double speed;
    private int recalc;

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
    public void stop() {
        tars.clearGoToTarget(true);
        tars.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos t = tars.getGoToTarget();
        if (t == null) return;
        if (--recalc <= 0) {
            recalc = 8;
            double spd = tars.isSprintMode() ? speed * 2.0 : speed;
            tars.getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, spd);
        }
    }
}
