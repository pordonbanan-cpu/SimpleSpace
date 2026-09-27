package com.simplespace.tars;

import com.simplespace.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Стыковка:
 * 1) блок-порт в обычном мире;
 * 2) сущность только из Sable / Simulated / Aeronautics (не мобы!).
 */
public class TarsDockGoal extends Goal {

    private final TarsEntity tars;
    private BlockPos dock;
    private UUID physicsVehicleId;
    private int recalc;
    private int settleTicks;
    private int failSpeakCd;

    public TarsDockGoal(TarsEntity tars) {
        this.tars = tars;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!tars.isDockOrdered()) return false;
        if (tars.isMiningOrdered()) return false;
        dock = findNearestDock();
        if (dock != null) {
            physicsVehicleId = null;
            return true;
        }
        Entity phys = findNearestPhysicsEntity();
        if (phys != null) {
            physicsVehicleId = phys.getUUID();
            return true;
        }
        // Нечего стыковать — один раз сказать
        if (--failSpeakCd <= 0) {
            failSpeakCd = 80;
            var o = tars.getOwner();
            if (o instanceof ServerPlayer sp)
                tars.speak(sp, "Порт не найден. Поставьте порт или соберите борт с портом рядом.");
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isDockOrdered()) return false;
        if (dock != null) {
            return tars.level().getBlockState(dock).is(ModBlocks.TARS_DOCK.get());
        }
        if (physicsVehicleId != null) {
            Entity e = findEntityById(physicsVehicleId);
            return e != null && e.isAlive() && isPhysicsLike(e);
        }
        return false;
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
            if (tars.isPassenger()) tars.stopRiding();
        }
        dock = null;
        physicsVehicleId = null;
    }

    @Override
    public void tick() {
        if (dock != null) {
            tickWorldDock();
            return;
        }
        if (physicsVehicleId != null) {
            tickPhysicsDock();
        }
    }

    private void tickWorldDock() {
        double tx = dock.getX() + 0.5;
        double ty = dock.getY() + 0.20;
        double tz = dock.getZ() + 0.45;
        double dist = tars.distanceToSqr(tx, ty, tz);

        if (dist <= 2.25) {
            snapDocked(tx, ty, tz, 180f);
            return;
        }

        tars.setDocked(false);
        tars.setNoGravity(false);
        if (--recalc <= 0) {
            recalc = 6;
            Path path = tars.getNavigation().createPath(dock, 0);
            if (path != null) tars.getNavigation().moveTo(path, 1.1);
            else tars.getNavigation().moveTo(tx, ty, tz, 1.1);
        }
    }

    private void tickPhysicsDock() {
        Entity vehicle = findEntityById(physicsVehicleId);
        if (vehicle == null || !vehicle.isAlive() || !isPhysicsLike(vehicle)) {
            physicsVehicleId = null;
            tars.setDocked(false);
            tars.setNoGravity(false);
            if (tars.isPassenger()) tars.stopRiding();
            return;
        }

        double dist = tars.distanceToSqr(vehicle);
        if (dist > 16.0) {
            tars.setDocked(false);
            tars.setNoGravity(false);
            if (--recalc <= 0) {
                recalc = 8;
                tars.getNavigation().moveTo(vehicle, 1.15);
            }
            return;
        }

        tars.getNavigation().stop();
        // Не садимся на мобов — только tryRide на contraption-entity
        if (!tars.isPassenger()) {
            boolean rode = false;
            try {
                rode = tars.startRiding(vehicle, true);
            } catch (Exception ignored) {}
            if (!rode) {
                softAttach(vehicle);
            } else {
                tars.setDocked(true);
                tars.setNoGravity(true);
            }
        } else {
            // Если вдруг сели не на то — слезть
            Entity v = tars.getVehicle();
            if (v != null && !isPhysicsLike(v)) {
                tars.stopRiding();
                tars.setDocked(false);
                return;
            }
            tars.setDocked(true);
            tars.setNoGravity(true);
            tars.setDeltaMovement(0, 0, 0);
        }

        if (settleTicks++ == 5) {
            var o = tars.getOwner();
            if (o instanceof ServerPlayer sp)
                tars.speak(sp, "Стыковка с физическим бортом.");
        }
    }

    private void softAttach(Entity vehicle) {
        tars.setDocked(true);
        tars.setNoGravity(true);
        tars.setDeltaMovement(vehicle.getDeltaMovement());
        tars.setPos(vehicle.getX(), vehicle.getY() + 0.35, vehicle.getZ());
        tars.setYRot(vehicle.getYRot());
        tars.setYBodyRot(vehicle.getYRot());
        tars.setYHeadRot(vehicle.getYRot());
        tars.setXRot(0f);
        tars.xxa = 0;
        tars.zza = 0;
        tars.setJumping(false);
    }

    private void snapDocked(double tx, double ty, double tz, float yaw) {
        tars.getNavigation().stop();
        tars.setDocked(true);
        tars.setNoGravity(true);
        tars.setDeltaMovement(0, 0, 0);
        tars.setPos(tx, ty, tz);
        tars.setYRot(yaw);
        tars.setYBodyRot(yaw);
        tars.setYHeadRot(yaw);
        tars.setXRot(0f);
        tars.xxa = 0;
        tars.zza = 0;
        tars.setJumping(false);
        if (settleTicks++ == 5) {
            var o = tars.getOwner();
            if (o instanceof ServerPlayer sp)
                tars.speak(sp, "В порту. Готов к отстою.");
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

    private Entity findNearestPhysicsEntity() {
        AABB box = tars.getBoundingBox().inflate(12);
        List<Entity> list = tars.level().getEntities(tars, box, this::isPhysicsLike);
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : list) {
            double d = e.distanceToSqr(tars);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** Только сущности физики Aeronautics/Sable — никогда мобы/игроки. */
    private boolean isPhysicsLike(Entity e) {
        if (e == null || !e.isAlive() || e == tars) return false;
        if (e instanceof Player) return false;
        if (e instanceof Mob) return false; // слизни, коровы и т.д. — нет
        if (e instanceof TarsEntity) return false;

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (key == null) return false;
        String ns = key.getNamespace();
        String path = key.getPath();

        if (ns.equals("sable") || ns.equals("simulated")
                || ns.equals("aeronautics") || ns.equals("create_aeronautics")) {
            return true;
        }
        if (ns.equals("create") && (path.contains("contraption") || path.contains("seat"))) {
            return true;
        }
        return path.contains("contraption") || path.contains("sub_level")
                || path.contains("physics_body") || path.contains("assembly");
    }

    private Entity findEntityById(UUID id) {
        if (id == null) return null;
        for (Entity e : tars.level().getEntities(tars, tars.getBoundingBox().inflate(48),
                ent -> id.equals(ent.getUUID()))) {
            return e;
        }
        return null;
    }
}
