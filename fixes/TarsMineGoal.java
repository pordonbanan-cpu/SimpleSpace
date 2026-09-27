package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class TarsMineGoal extends Goal {

    private final TarsEntity tars;
    private BlockPos target;
    private int digCooldown;
    private int repathCooldown;
    private int noProgressTicks;
    private double lastDist = Double.MAX_VALUE;

    public TarsMineGoal(TarsEntity tars) {
        this.tars = tars;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (tars.getFlashLevel() < 2) return false;
        if (!tars.isMiningOrdered()) return false;
        target = tars.findNearestOre(tars.getMineOreFilter());
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isMiningOrdered() || tars.getFlashLevel() < 2) return false;
        if (target == null) return false;
        if (tars.level().getBlockState(target).isAir()) {
            target = tars.findNearestOre(tars.getMineOreFilter());
            lastDist = Double.MAX_VALUE;
            return target != null;
        }
        return true;
    }

    @Override
    public void start() {
        digCooldown = 0;
        repathCooldown = 0;
        noProgressTicks = 0;
        lastDist = Double.MAX_VALUE;
        tars.setDigging(false);
    }

    @Override
    public void stop() {
        tars.setDigging(false);
        tars.getNavigation().stop();
        target = null;
    }

    @Override
    public void tick() {
        if (target == null) return;
        double dist = tars.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        tars.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (dist < lastDist - 0.05) { noProgressTicks = 0; lastDist = dist; }
        else noProgressTicks++;

        if (dist <= 6.25) {
            tars.getNavigation().stop();
            tars.setDigging(true);
            if (--digCooldown <= 0) {
                digCooldown = tars.getFlashLevel() >= 3 ? 7 : 11;
                if (!tars.level().getBlockState(target).isAir()) {
                    tars.breakBlockForMine(target);
                }
                target = tars.findNearestOre(tars.getMineOreFilter());
                lastDist = Double.MAX_VALUE;
                if (target == null) {
                    tars.setMiningOrdered(false);
                    tars.setDigging(false);
                    ServerPlayer sp = owner();
                    if (sp != null) tars.speak(sp, "Готово. На складе: " + tars.countItems() + " шт.");
                }
            }
            return;
        }
        if (--repathCooldown <= 0) {
            repathCooldown = 6;
            BlockPos walkTo = target;
            if (target.getY() < tars.blockPosition().getY() - 1)
                walkTo = new BlockPos(target.getX(), tars.blockPosition().getY(), target.getZ());
            Path path = tars.getNavigation().createPath(walkTo, 1);
            if (path == null) path = tars.getNavigation().createPath(target, 0);
            if (path != null) {
                tars.getNavigation().moveTo(path, tars.getFlashLevel() >= 3 ? 1.4 : 1.2);
                tars.setDigging(false);
            }
        }
        if (noProgressTicks > 12 || dist < 64) digToward(target);
    }

    private void digToward(BlockPos goal) {
        if (--digCooldown > 0) return;
        digCooldown = tars.getFlashLevel() >= 3 ? 5 : 8;
        tars.setDigging(true);
        BlockPos base = tars.blockPosition();
        int dx = Integer.signum(goal.getX() - base.getX());
        int dy = Integer.signum(goal.getY() - base.getY());
        int dz = Integer.signum(goal.getZ() - base.getZ());
        BlockPos[] candidates = new BlockPos[] {
            base.offset(dx, 0, dz), base.offset(dx, 1, dz), base.offset(dx, dy, dz),
            base.offset(0, dy, 0), base.offset(dx, -1, dz), base.above(), goal
        };
        for (BlockPos p : candidates) {
            BlockState st = tars.level().getBlockState(p);
            if (st.isAir() || st.getDestroySpeed(tars.level(), p) < 0) continue;
            if (st.is(Blocks.BEDROCK) || st.is(Blocks.BARRIER)) continue;
            tars.breakBlockForMine(p);
            noProgressTicks = 0;
            return;
        }
    }

    private ServerPlayer owner() {
        var o = tars.getOwner();
        return o instanceof ServerPlayer sp ? sp : null;
    }
}
