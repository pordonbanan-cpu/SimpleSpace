package com.simplespace.tars;

import com.mojang.logging.LogUtils;
import com.simplespace.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Стыковка:
 * 1) блок-порт в обычном мире;
 * 2) сущность физического тела Sable / Aeronautics (не мобы!).
 *
 * ИСПРАВЛЕНО: раньше поиск физического тела шёл только в радиусе 12 блоков
 * вокруг TARS (AABB.inflate(12)). У физдвижков вроде Sable координата самой
 * сущности корабля — это её центр масс/якорь, а не место, где реально стоят
 * блоки конструкции. Из-за этого сущность корабля могла быть в 20-50+ блоках
 * от места, где TARS физически стоит на порту, и старый радиус её просто не
 * находил. Теперь сначала ищем без ограничения радиуса по всему загруженному
 * уровню, а если ничего не находится вообще — говорим об этом прямо (а не
 * молча "порт не найден"), чтобы было видно, действительно ли дело в типе
 * сущности или в чём-то другом.
 */
public class TarsDockGoal extends Goal {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Если физическое тело нашлось дальше — TARS просто идёт к нему дольше. */
    private static final double MAX_PHYSICS_SEARCH_RANGE = 256.0;
    /** Дистанция, на которой TARS уже пробует сесть верхом. */
    private static final double MOUNT_RANGE_SQR = 36.0; // 6 блоков
    /** Радиус диагностического лога вокруг TARS, когда стыковка не удаётся. */
    private static final double DEBUG_LOG_RADIUS = 48.0;

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
        // В чат ничего не пишем. Раз в ~4 сек тихо логируем на сервер, что
        // видно вокруг TARS — это единственный способ узнать точный ID
        // сущности физ. платформы, если она не распознаётся.
        if (--failSpeakCd <= 0) {
            failSpeakCd = 80;
            logNearbyEntities();
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
        if (dist > MOUNT_RANGE_SQR) {
            tars.setDocked(false);
            tars.setNoGravity(false);
            if (--recalc <= 0) {
                recalc = 8;
                // Прямой полёт к цели без pathfinding — по воздуху/физ. платформе
                // pathfinding часто не строится (блоки не в обычном мире),
                // поэтому дублируем moveTo и прямым вектором на всякий случай.
                Path path = tars.getNavigation().createPath(vehicle, 0);
                if (path != null) {
                    tars.getNavigation().moveTo(path, 1.15);
                } else {
                    tars.getNavigation().moveTo(vehicle, 1.15);
                }
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

    /**
     * Раньше искали только в AABB.inflate(12) вокруг TARS. Физ. сущность
     * (центр масс корабля) может быть далеко от блоков конструкции, поэтому
     * теперь ищем по всем сущностям загруженного уровня (без привязки к
     * позиции TARS), ограничивая только большим потолком дальности, чтобы не
     * зацепить что-то из другого конца карты.
     */
    private Entity findNearestPhysicsEntity() {
        List<Entity> list = tars.level().getEntities(tars,
                new AABB(-3.0E7, -3.0E7, -3.0E7, 3.0E7, 3.0E7, 3.0E7),
                this::isPhysicsLike);
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : list) {
            double d = e.distanceToSqr(tars);
            if (d > MAX_PHYSICS_SEARCH_RANGE * MAX_PHYSICS_SEARCH_RANGE) continue;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /**
     * Раз в ~4 сек, если стыковка не удалась, тихо пишет в лог сервера
     * (latest.log / logs/latest.log) список всех сущностей в радиусе
     * DEBUG_LOG_RADIUS вокруг TARS — с их точным registry ID и Java-классом.
     * Ничего не пишет в чат игроку. Это нужно один раз: чтобы увидеть
     * реальный ID физ. платформы и точно прописать его в isPhysicsLike().
     */
    private void logNearbyEntities() {
        List<Entity> nearby = tars.level().getEntities(tars,
                tars.getBoundingBox().inflate(DEBUG_LOG_RADIUS),
                e -> e != tars && !(e instanceof Player));
        if (nearby.isEmpty()) {
            LOGGER.info("[TARS-DOCK-DEBUG] Рядом с TARS ({}) вообще нет сущностей в радиусе {} блоков.",
                    tars.blockPosition(), DEBUG_LOG_RADIUS);
            return;
        }
        StringBuilder sb = new StringBuilder("[TARS-DOCK-DEBUG] Сущности рядом с TARS (")
                .append(tars.blockPosition()).append("), радиус ").append(DEBUG_LOG_RADIUS).append(":");
        for (Entity e : nearby) {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            sb.append("\n  - id=").append(key)
                    .append(" class=").append(e.getClass().getName())
                    .append(" pos=").append(e.position())
                    .append(" distSqr=").append(String.format("%.1f", e.distanceToSqr(tars)))
                    .append(" isPhysicsLike=").append(isPhysicsLike(e));
        }
        LOGGER.info(sb.toString());
    }

    /**
     * Физ. тела Sable/Aeronautics/Create-контрапшены.
     * Проверяем И по registry ID, И по Java-классу/пакету — некоторые сборки
     * Create Aeronautics регистрируют сущность под неочевидным ID, но класс
     * почти всегда содержит "Contraption" или лежит в пакете dev.ryanhcode.sable
     * либо в пакете конкретно этого аддона.
     */
    private boolean isPhysicsLike(Entity e) {
        if (e == null || !e.isAlive() || e == tars) return false;
        if (e instanceof Player) return false;
        if (e instanceof TarsEntity) return false;

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        String ns = key == null ? "" : key.getNamespace();
        String path = key == null ? "" : key.getPath();

        if (ns.equals("sable") || ns.equals("simulated")
                || ns.equals("aeronautics") || ns.equals("create_aeronautics")) {
            return true;
        }
        if (ns.equals("create") && (path.contains("contraption") || path.contains("seat"))) {
            return true;
        }
        if (path.contains("contraption") || path.contains("sub_level")
                || path.contains("physics_body") || path.contains("assembly")
                || path.contains("ship") || path.contains("vehicle")) {
            return true;
        }

        // Мобы обычно отсекаем, но контрапшен-сущности Create технически
        // тоже наследуются не от Mob, так что эта проверка идёт ПОСЛЕ
        // проверок выше и не мешает им.
        if (e instanceof Mob) return false;

        String className = e.getClass().getName();
        return className.contains("Contraption")
                || className.startsWith("dev.ryanhcode.sable")
                || className.contains("Sable")
                || className.contains("PhysicsBody")
                || className.contains("SubLevel");
    }

    private Entity findEntityById(UUID id) {
        if (id == null) return null;
        for (Entity e : tars.level().getEntities(tars,
                new AABB(-3.0E7, -3.0E7, -3.0E7, 3.0E7, 3.0E7, 3.0E7),
                ent -> id.equals(ent.getUUID()))) {
            return e;
        }
        return null;
    }
}