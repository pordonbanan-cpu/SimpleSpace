package com.simplespace.tars;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class TarsLookAtOwnerGoal extends Goal {

    private final TarsEntity tars;

    public TarsLookAtOwnerGoal(TarsEntity tars) {
        this.tars = tars;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!tars.isFollowing()) return false;
        Player o = tars.getOwner();
        return o != null && o.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        Player o = tars.getOwner();
        if (o != null) {
            tars.getLookControl().setLookAt(o, 40.0f, 40.0f);
        }
    }
}
