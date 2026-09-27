package com.simplespace.tars;

import com.simplespace.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Стыковка:
 * 1) обычный блок-порт в мире;
 * 2) если порта нет (уже в Physics Assembler) — «мягкая» привязка
 *    к ближайшей сущности Sable/Simulated/Create Aeronautics.
 */
public class TarsDockGoal extends Goal {

    private final TarsEntity tars;
    private BlockPos dock;
    private UUID physicsVehicleId;
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
        if (dock != null) return true;
        // Порт уже «физический» — ищем сущность-носитель
        Entity phys = findNearestPhysicsEntity();
        if (phys != null) {
            physicsVehicleId = phys.getUUID();
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!tars.isDockOrdered()) return false;
        if (dock != null) {
            BlockState st = tars.level().getBlockState(dock);
            return st.is(ModBlocks.TARS_DOCK.get());
        }
        if (physicsVehicleId != null) {
            Entity e = findEntityById(physicsVehicleId);
            return e != null && e.isAlive();
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
        if (vehicle == null || !vehicle.isAlive()) {
            physicsVehicleId = null;
            tars.setDocked(false);
            tars.setNoGravity(false);
            return;
        }

        double dist = tars.distanceToSqr(vehicle);
        // Подойти ближе
        if (dist > 9.0) {
            tars.setDocked(false);
            tars.setNoGravity(false);
            if (--recalc <= 0) {
                recalc = 8;
                tars.getNavigation().moveTo(vehicle, 1.15);
            }
            return;
        }

        tars.getNavigation().stop();

        // Пробуем сесть пассажиром (Create seat / vehicle entity)
        if (!tars.isPassenger()) {
            boolean rode = tars.startRiding(vehicle, true);
            if (!rode) {
                // Мягкая привязка: копируем позицию/скорость носителя
                softAttach(vehicle);
            } else {
                tars.setDocked(true);
                tars.setNoGravity(true);
            }
        } else {
            tars.setDocked(true);
            tars.setNoGravity(true);
            tars.setDeltaMovement(0, 0, 0);
        }

        if (settleTicks++ == 5) {
            var o = tars.getOwner();
            if (o instanceof net.minecraft.server.level.ServerPlayer sp)
                tars.speak(sp, "Стыковка с физическим бортом. Готов к перелёту.");
        }
    }

    private void softAttach(Entity vehicle) {
        // Смещение «в порт» относительно носителя
        double ox = vehicle.getX();
        double oy = vehicle.getY() + 0.35;
        double oz = vehicle.getZ();
        tars.setDocked(true);
        tars.setNoGravity(true);
        tars.setDeltaMovement(vehicle.getDeltaMovement());
        tars.setPos(ox, oy, oz);
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
            if (o instanceof net.minecraft.server.level.ServerPlayer sp)
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
        if (!ModList.get().isLoaded("sable")
                && !ModList.get().isLoaded("simulated")
                && !ModList.get().isLoaded("aeronautics")
                && !ModList.get().isLoaded("create_aeronautics")) {
            // всё равно ищем крупные entity — вдруг другой id мода
        }
        AABB box = tars.getBoundingBox().inflate(16);
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

    private boolean isPhysicsLike(Entity e) {
        if (e == null || !e.isAlive() || e == tars) return false;
        if (e instanceof net.minecraft.world.entity.player.Player) return false;
        if (e instanceof TarsEntity) return false;
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        if (key == null) return false;
        String ns = key.getNamespace();
        String path = key.getPath();
        // Sable / Simulated / Aeronautics / Create contraption entities
        if (ns.equals("sable") || ns.equals("simulated") || ns.equals("aeronautics")
                || ns.equals("create_aeronautics") || ns.equals("create")) {
            return true;
        }
        if (path.contains("contraption") || path.contains("sub_level")
                || path.contains("physics") || path.contains("assembly")
                || path.contains("seat")) {
            return true;
        }
        // Крупные сущности рядом с портом — запасной вариант
        return e.getBbWidth() >= 1.2 || e.getBbHeight() >= 1.2;
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
