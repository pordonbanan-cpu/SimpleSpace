package com.simplespace.tars;

import com.simplespace.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class TarsDockGoal extends Goal {

    private final TarsEntity tars;
    private BlockPos dock;
    private int recalc;
    private int settleTicks;

    public TarsDockGoal(TarsEntity tars) {
        this.tars = tars;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!tars.isDockOrdered()) return false;
        if (tars.isMiningOrdered()) return false;
        dock = findNearestDock();
        return dock != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isDockOrdered()) return false;
        if (dock == null) return false;
        BlockState st = tars.level().getBlockState(dock);
        if (!st.is(ModBlocks.TARS_DOCK.get())) return false;
        return true;
    }

    @Override
    public void start() {
        recalc = 0;
        settleTicks = 0;
        tars.setFollowing(false);
        tars.setSprintMode(false);
        tars.setDigging(false);
        tars.setClimbing(false);
    }

    @Override
    public void stop() {
        tars.getNavigation().stop();
        if (!tars.isDockOrdered()) {
            tars.setDocked(false);
        }
        dock = null;
    }

    @Override
    public void tick() {
        if (dock == null) return;
        double dist = tars.distanceToSqr(dock.getX() + 0.5, dock.getY() + 0.25, dock.getZ() + 0.5);

        if (dist <= 1.6) {
            tars.getNavigation().stop();
            tars.setDocked(true);
            tars.setPos(dock.getX() + 0.5, dock.getY() + 0.25, dock.getZ() + 0.5);
            tars.setYRot(tars.getYRot());
            tars.setDeltaMovement(0, 0, 0);
            if (settleTicks++ == 5) {
                var o = tars.getOwner();
                if (o instanceof net.minecraft.server.level.ServerPlayer sp)
                    tars.speak(sp, "В порту. Готов к отстою.");
            }
            return;
        }

        tars.setDocked(false);
        if (--recalc <= 0) {
            recalc = 8;
            Path path = tars.getNavigation().createPath(dock, 0);
            if (path != null) tars.getNavigation().moveTo(path, 1.15);
        }
    }

    private BlockPos findNearestDock() {
        BlockPos origin = tars.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int r = 32;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!tars.level().getBlockState(p).is(ModBlocks.TARS_DOCK.get())) continue;
                    double d = p.distSqr(origin);
                    if (d < bestD) {
                        bestD = d;
                        best = p.immutable();
                    }
                }
            }
        }
        return best;
    }
}
