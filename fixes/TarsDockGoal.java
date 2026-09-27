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
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
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
        return st.is(ModBlocks.TARS_DOCK.get());
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
        tars.setNoGravity(false);
        if (!tars.isDockOrdered()) {
            tars.setDocked(false);
        }
        dock = null;
    }

    @Override
    public void tick() {
        if (dock == null) return;

        // Центр площадки, чуть выше пола (3/16 блока)
        double tx = dock.getX() + 0.5;
        double ty = dock.getY() + 0.20;
        double tz = dock.getZ() + 0.45; // чуть к открытой стороне (север модели — front lip)

        double dist = tars.distanceToSqr(tx, ty, tz);

        if (dist <= 2.25) {
            tars.getNavigation().stop();
            tars.setDocked(true);
            tars.setNoGravity(true);
            tars.setDeltaMovement(0, 0, 0);
            tars.setPos(tx, ty, tz);
            // Лицом к открытой стороне порта (юг → смотрит наружу с front lip)
            tars.setYRot(180f);
            tars.setYBodyRot(180f);
            tars.setYHeadRot(180f);
            tars.setXRot(0f);
            tars.xxa = 0;
            tars.zza = 0;
            tars.setJumping(false);

            if (settleTicks++ == 5) {
                var o = tars.getOwner();
                if (o instanceof net.minecraft.server.level.ServerPlayer sp)
                    tars.speak(sp, "В порту. Готов к отстою.");
            }
            return;
        }

        tars.setDocked(false);
        tars.setNoGravity(false);
        if (--recalc <= 0) {
            recalc = 6;
            // path к блоку рядом с портом, затем финальный snap
            Path path = tars.getNavigation().createPath(dock, 0);
            if (path != null) {
                tars.getNavigation().moveTo(path, 1.1);
            } else {
                tars.getNavigation().moveTo(tx, ty, tz, 1.1);
            }
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
