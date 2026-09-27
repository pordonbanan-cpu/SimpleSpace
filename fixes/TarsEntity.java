package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Locale;
import java.util.UUID;

public class TarsEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Integer> HUMOR =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FOLLOWING =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPRINT_MODE =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FLASH_LEVEL =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLASH_PROGRESS =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DIGGING =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);

    public static final double AUTO_ROLL_DISTANCE = 14.0;
    public static final int FLASH_INSTALL_TICKS = 80;
    public static final int ORE_SEARCH_RADIUS = 24;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUUID;
    private int jokeCooldown;
    private int walkSoundCooldown;
    private int rollSoundCooldown;
    private float lockedYRot = Float.NaN;
    private boolean wasMoving;
    private boolean autoSprintActive;
    private BlockPos goToTarget;
    private int pendingFlashLevel;
    private boolean miningOrdered;
    private String mineOreFilter;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation DIG = RawAnimation.begin().thenLoop("dig");

    private static final String[] JOKES = {
            "Это была шутка.", "Могу повторить с меньшей честностью.",
            "Статистически вы должны были уже упасть.",
            "Я пересчитал. Всё ещё плохая идея.",
            "Юмор включён. Результат… спорный.", "Коопера. Снова.",
            "Модули в норме. Ваш план — нет.",
            "Я не спорю. Я сообщаю вероятность.",
            "Если это приказ — он принят. Если шутка — она слабая.",
            "Честность 90%. Остальное — такт.",
            "Вы просили сопровождение. Улыбка не входила в спецификацию.",
            "Гравитация не спрашивает. Я тоже."
    };

    public TarsEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.jokeCooldown = 200 + random.nextInt(200);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUMOR, 75);
        builder.define(FOLLOWING, true);
        builder.define(SPRINT_MODE, false);
        builder.define(FLASH_LEVEL, 0);
        builder.define(FLASH_PROGRESS, 0);
        builder.define(DIGGING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TarsMineGoal(this));
        this.goalSelector.addGoal(2, new TarsGoToGoal(this, 1.3));
        this.goalSelector.addGoal(3, new TarsFollowOwnerGoal(this, 1.25, 1.5f, 2.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !TarsEntity.this.isFollowing()
                        && TarsEntity.this.getGoToTarget() == null
                        && !TarsEntity.this.isMiningOrdered()
                        && super.canUse();
            }
        });
    }

    public int getHumor() { return this.entityData.get(HUMOR); }
    public void setHumor(int v) { this.entityData.set(HUMOR, Math.max(0, Math.min(100, v))); }
    public boolean isFollowing() { return this.entityData.get(FOLLOWING); }
    public int getFlashLevel() { return this.entityData.get(FLASH_LEVEL); }
    public void setFlashLevel(int v) { this.entityData.set(FLASH_LEVEL, Math.max(0, Math.min(3, v))); }
    public int getFlashProgress() { return this.entityData.get(FLASH_PROGRESS); }
    public boolean isDigging() { return this.entityData.get(DIGGING); }
    public void setDigging(boolean v) { this.entityData.set(DIGGING, v); }
    public boolean isMiningOrdered() { return miningOrdered; }
    public void setMiningOrdered(boolean v) {
        miningOrdered = v;
        if (!v) { mineOreFilter = null; setDigging(false); }
    }
    public String getMineOreFilter() { return mineOreFilter; }

    public void setFollowing(boolean v) {
        boolean prev = isFollowing();
        this.entityData.set(FOLLOWING, v);
        if (v) {
            this.goToTarget = null;
            this.miningOrdered = false;
            this.lockedYRot = Float.NaN;
            if (!prev && !level().isClientSide()) playLocal(SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.3f);
        } else {
            this.lockedYRot = this.getYRot();
            if (prev && !level().isClientSide()) playLocal(SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.2f);
        }
    }

    public boolean isSprintMode() { return this.entityData.get(SPRINT_MODE); }

    public void setSprintMode(boolean v) {
        boolean prev = isSprintMode();
        this.entityData.set(SPRINT_MODE, v);
        if (!v) autoSprintActive = false;
        if (!level().isClientSide() && v != prev) {
            if (v) playLocal(SoundEvents.PISTON_EXTEND, 0.7f, 0.8f);
            else playLocal(SoundEvents.PISTON_CONTRACT, 0.7f, 0.9f);
        }
    }

    public BlockPos getGoToTarget() { return goToTarget; }

    public void setGoToTarget(BlockPos pos) {
        this.goToTarget = pos;
        if (pos != null) {
            this.entityData.set(FOLLOWING, false);
            this.entityData.set(SPRINT_MODE, false);
            this.autoSprintActive = false;
            this.miningOrdered = false;
            this.lockedYRot = Float.NaN;
            this.getNavigation().stop();
        }
    }

    public void clearGoToTarget(boolean announce) {
        if (goToTarget == null) return;
        goToTarget = null;
        if (announce && !level().isClientSide()) {
            Player o = getOwner();
            if (o instanceof ServerPlayer sp) {
                speak(sp, "На месте.");
                playLocal(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f, 1.2f);
            }
        }
    }

    public boolean beginFlashInstall(int level, ServerPlayer sp) {
        if (level <= getFlashLevel()) {
            speak(sp, "Уже установлен уровень " + getFlashLevel() + " или выше.");
            return false;
        }
        if (getFlashProgress() > 0) {
            speak(sp, "Перепрошивка уже идёт.");
            return false;
        }
        pendingFlashLevel = level;
        this.entityData.set(FLASH_PROGRESS, 1);
        speak(sp, "Установка модуля L" + level + "…");
        playLocal(SoundEvents.BEACON_ACTIVATE, 0.5f, 1.4f);
        return true;
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public void setOwnerUUID(UUID uuid) { this.ownerUUID = uuid; }

    public Player getOwner() {
        if (ownerUUID == null || level().isClientSide()) return null;
        return level().getPlayerByUUID(ownerUUID);
    }

    public void speak(ServerPlayer to, String line) {
        to.sendSystemMessage(Component.literal("§8[TARS] §f" + line));
    }

    private void playLocal(net.minecraft.sounds.SoundEvent sound, float vol, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, vol, pitch);
    }

    public static boolean matchesOreFilter(BlockState st, String filter) {
        if (!isOre(st)) return false;
        if (filter == null || filter.isEmpty()) return true;
        String f = filter.toLowerCase(Locale.ROOT);
        Block b = st.getBlock();
        String key = b.getDescriptionId().toLowerCase(Locale.ROOT);
        return switch (f) {
            case "iron", "железо", "желез" -> st.is(BlockTags.IRON_ORES) || key.contains("iron");
            case "coal", "уголь", "угл" -> st.is(BlockTags.COAL_ORES) || key.contains("coal");
            case "gold", "золото", "золот" -> st.is(BlockTags.GOLD_ORES) || key.contains("gold");
            case "diamond", "алмаз" -> st.is(BlockTags.DIAMOND_ORES) || key.contains("diamond");
            case "copper", "медь", "мед" -> st.is(BlockTags.COPPER_ORES) || key.contains("copper");
            case "redstone", "редстоун" -> st.is(BlockTags.REDSTONE_ORES) || key.contains("redstone");
            case "lapis", "лазурит" -> st.is(BlockTags.LAPIS_ORES) || key.contains("lapis");
            case "emerald", "изумруд" -> st.is(BlockTags.EMERALD_ORES) || key.contains("emerald");
            case "quartz", "кварц" -> b == Blocks.NETHER_QUARTZ_ORE || key.contains("quartz");
            case "debris", "незерит", "ancient" -> b == Blocks.ANCIENT_DEBRIS || key.contains("debris");
            default -> key.contains(f);
        };
    }

    public static boolean isOre(BlockState st) {
        if (st.isAir()) return false;
        if (st.is(BlockTags.COAL_ORES) || st.is(BlockTags.IRON_ORES) || st.is(BlockTags.COPPER_ORES)
                || st.is(BlockTags.GOLD_ORES) || st.is(BlockTags.REDSTONE_ORES)
                || st.is(BlockTags.LAPIS_ORES) || st.is(BlockTags.DIAMOND_ORES)
                || st.is(BlockTags.EMERALD_ORES)) return true;
        Block b = st.getBlock();
        return b == Blocks.NETHER_QUARTZ_ORE || b == Blocks.NETHER_GOLD_ORE || b == Blocks.ANCIENT_DEBRIS;
    }

    public BlockPos findNearestOre(String filter) {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        int r = ORE_SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -8; dy <= 8; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (!matchesOreFilter(level().getBlockState(p), filter)) continue;
                    double d = p.distSqr(origin);
                    if (d < bestD) { bestD = d; best = p.immutable(); }
                }
            }
        }
        return best;
    }

    public static String parseOreType(String msg) {
        String m = msg.toLowerCase(Locale.ROOT);
        if (m.contains("алмаз") || m.contains("diamond")) return "diamond";
        if (m.contains("желез") || m.contains("iron")) return "iron";
        if (m.contains("золот") || m.contains("gold")) return "gold";
        if (m.contains("угл") || m.contains("coal")) return "coal";
        if (m.contains("мед") || m.contains("copper")) return "copper";
        if (m.contains("редстоун") || m.contains("redstone")) return "redstone";
        if (m.contains("лазурит") || m.contains("lapis")) return "lapis";
        if (m.contains("изумруд") || m.contains("emerald")) return "emerald";
        if (m.contains("кварц") || m.contains("quartz")) return "quartz";
        if (m.contains("незерит") || m.contains("debris") || m.contains("ancient")) return "debris";
        return null;
    }

    public boolean handleVoiceCommand(ServerPlayer sp, String raw) {
        if (ownerUUID != null && !ownerUUID.equals(sp.getUUID())) return false;
        if (ownerUUID == null) setOwnerUUID(sp.getUUID());

        String msg = raw.toLowerCase(Locale.ROOT).trim();
        if (msg.startsWith("tars ")) msg = msg.substring(5).trim();
        if (msg.startsWith("тарс ")) msg = msg.substring(5).trim();

        if (containsAny(msg, "где ближайш", "где руда", "где руды", "nearest ore", "найди руд")) {
            if (getFlashLevel() < 1) { speak(sp, "Нужен модуль L1+."); return true; }
            String filter = parseOreType(msg);
            BlockPos p = findNearestOre(filter);
            if (p == null) speak(sp, filter == null ? "Руда рядом не найдена." : "Такая руда рядом не найдена.");
            else speak(sp, "Ближайшая" + (filter != null ? " (" + filter + ")" : "")
                    + ": " + p.getX() + " " + p.getY() + " " + p.getZ()
                    + " §7(" + (int) Math.sqrt(p.distSqr(blockPosition())) + "м)");
            return true;
        }

        if (containsAny(msg, "добудь", "добывай", "копай", "mine ", "добыть")) {
            if (getFlashLevel() < 2) { speak(sp, "Для добычи нужен модуль L2+."); return true; }
            mineOreFilter = parseOreType(msg);
            miningOrdered = true;
            setFollowing(false);
            setSprintMode(false);
            clearGoToTarget(false);
            BlockPos p = findNearestOre(mineOreFilter);
            if (p == null) {
                miningOrdered = false;
                speak(sp, "Руда для добычи не найдена.");
            } else {
                speak(sp, "Добываю" + (mineOreFilter != null ? " (" + mineOreFilter + ")" : "")
                        + ". Цель: " + p.getX() + " " + p.getY() + " " + p.getZ());
            }
            return true;
        }

        if (containsAny(msg, "хватит копать", "стоп добыч", "не копай", "stop mine")) {
            setMiningOrdered(false);
            speak(sp, "Добыча остановлена.");
            return true;
        }

        Integer[] xyz = parseCoords(msg);
        if (xyz != null && (containsAny(msg, "иди", "go", "координат") || (countNumbers(msg) >= 3 && containsAny(msg, "на", "к")))) {
            BlockPos p = new BlockPos(xyz[0], xyz[1], xyz[2]);
            setGoToTarget(p);
            speak(sp, "Иду на " + p.getX() + " " + p.getY() + " " + p.getZ() + ".");
            return true;
        }

        if (containsAny(msg, "за мной", "следуй", "follow", "ко мне", "за мно")) {
            setMiningOrdered(false);
            setFollowing(true); setSprintMode(false); clearGoToTarget(false);
            speak(sp, "Иду за вами."); return true;
        }
        if (containsAny(msg, "стой", "стоять", "стоп", "stay", "жди", "ждать")) {
            setMiningOrdered(false);
            setFollowing(false); setSprintMode(false); clearGoToTarget(false);
            getNavigation().stop(); speak(sp, "Стою."); return true;
        }
        if (containsAny(msg, "беги", "перекат", "катись", "sprint", "roll", "бегом")) {
            setMiningOrdered(false);
            setFollowing(true); setSprintMode(true); clearGoToTarget(false);
            speak(sp, "Перекат."); return true;
        }
        return false;
    }

    private static boolean containsAny(String msg, String... keys) {
        for (String k : keys) if (msg.contains(k)) return true;
        return false;
    }

    private static int countNumbers(String msg) {
        int n = 0;
        for (String p : msg.replace(",", " ").split("\\s+"))
            if (p.matches("-?\\d+(\\.\\d+)?")) n++;
        return n;
    }

    private static Integer[] parseCoords(String msg) {
        String[] parts = msg.replace(",", " ").replace("~", "").split("\\s+");
        java.util.ArrayList<Integer> nums = new java.util.ArrayList<>();
        for (String p : parts) {
            try {
                if (p.matches("-?\\d+(\\.\\d+)?")) nums.add((int) Math.floor(Double.parseDouble(p)));
            } catch (Exception ignored) {}
        }
        if (nums.size() >= 3) return new Integer[]{nums.get(0), nums.get(1), nums.get(2)};
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        orientBody();
        tickAutoRoll();
        tickFlashInstall();
        tickMovementSounds();
        tickJokes();
        if (!miningOrdered && isDigging() && random.nextInt(10) == 0) setDigging(false);
    }

    private void tickFlashInstall() {
        int prog = getFlashProgress();
        if (prog <= 0) return;
        prog++;
        if (prog >= FLASH_INSTALL_TICKS) {
            setFlashLevel(pendingFlashLevel);
            this.entityData.set(FLASH_PROGRESS, 0);
            pendingFlashLevel = 0;
            playLocal(SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.2f);
            Player o = getOwner();
            if (o instanceof ServerPlayer sp)
                speak(sp, "Модуль L" + getFlashLevel() + " установлен. " + flashDesc(getFlashLevel()));
        } else {
            this.entityData.set(FLASH_PROGRESS, prog);
        }
    }

    public static String flashDesc(int level) {
        return switch (level) {
            case 1 -> "Сканер руд.";
            case 2 -> "Добыча руд.";
            case 3 -> "Быстрая добыча.";
            default -> "Базовая прошивка.";
        };
    }

    private void tickAutoRoll() {
        if (!isFollowing() || getGoToTarget() != null || miningOrdered) return;
        Player owner = getOwner();
        if (owner == null) return;
        double d = distanceTo(owner);
        if (d > AUTO_ROLL_DISTANCE) {
            if (!isSprintMode()) { setSprintMode(true); autoSprintActive = true; }
        } else if (d < AUTO_ROLL_DISTANCE * 0.55 && autoSprintActive) {
            setSprintMode(false); autoSprintActive = false;
        }
    }

    private void orientBody() {
        if (!isFollowing() && getGoToTarget() == null && !miningOrdered) {
            if (!Float.isNaN(lockedYRot)) {
                this.setYRot(lockedYRot); this.yBodyRot = lockedYRot; this.yHeadRot = lockedYRot;
            }
            return;
        }
        Player owner = getOwner();
        Vec3 motion = this.getDeltaMovement();
        double speed2 = motion.x * motion.x + motion.z * motion.z;
        boolean moving = speed2 > 0.0008 || !this.getNavigation().isDone();

        if (moving || miningOrdered) {
            double mx = motion.x, mz = motion.z;
            if (speed2 < 0.0001) {
                if (getGoToTarget() != null) {
                    mx = getGoToTarget().getX() + 0.5 - getX();
                    mz = getGoToTarget().getZ() + 0.5 - getZ();
                } else if (owner != null && isFollowing()) {
                    mx = owner.getX() - getX(); mz = owner.getZ() - getZ();
                }
            }
            if (mx * mx + mz * mz > 1.0E-6) {
                float target = (float) (Math.atan2(mz, mx) * (180F / Math.PI)) - 90.0F;
                float next = approachDegrees(this.getYRot(), target, isSprintMode() ? 6f : 10f);
                this.setYRot(next); this.yBodyRot = next; this.yHeadRot = next;
            }
        } else if (isFollowing() && owner != null && owner.isAlive()) {
            double dx = owner.getX() - getX(), dz = owner.getZ() - getZ();
            float target = (float) (Math.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
            float next = approachDegrees(this.getYRot(), target, 4f);
            this.setYRot(next); this.yBodyRot = next; this.yHeadRot = next;
        }
    }

    private void tickMovementSounds() {
        Vec3 motion = this.getDeltaMovement();
        boolean moving = motion.horizontalDistanceSqr() > 0.001 || !this.getNavigation().isDone();
        if (isSprintMode() && moving) {
            if (--rollSoundCooldown <= 0) {
                rollSoundCooldown = 8;
                playLocal(SoundEvents.ELYTRA_FLYING, 0.35f, 0.6f + random.nextFloat() * 0.15f);
            }
        } else if (moving && (isFollowing() || getGoToTarget() != null || miningOrdered)) {
            if (--walkSoundCooldown <= 0) {
                walkSoundCooldown = 12 + random.nextInt(4);
                playLocal(SoundEvents.IRON_GOLEM_STEP, 0.4f, 1.1f + random.nextFloat() * 0.2f);
            }
        }
        if (wasMoving && !moving && isSprintMode()) playLocal(SoundEvents.IRON_GOLEM_DAMAGE, 0.3f, 1.5f);
        wasMoving = moving;
    }

    private void tickJokes() {
        if (getHumor() <= 0) return;
        if (--jokeCooldown > 0) return;
        jokeCooldown = 300 + random.nextInt(400);
        if (random.nextFloat() > getHumor() / 100f * 0.55f) return;
        Player owner = getOwner();
        if (owner instanceof ServerPlayer sp && distanceToSqr(sp) < 48 * 48) {
            speak(sp, JOKES[random.nextInt(JOKES.length)]);
            playLocal(SoundEvents.NOTE_BLOCK_BIT.value(), 0.5f, 0.8f + random.nextFloat() * 0.4f);
        }
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float d = net.minecraft.util.Mth.wrapDegrees(target - current);
        if (d > maxDelta) d = maxDelta;
        if (d < -maxDelta) d = -maxDelta;
        return current + d;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (ownerUUID != null && !ownerUUID.equals(player.getUUID())) {
            if (!level().isClientSide() && player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§c[TARS] Не ваш робот."));
            return InteractionResult.CONSUME;
        }
        if (ownerUUID == null) setOwnerUUID(player.getUUID());
        // Flash ONLY via menu
        if (player.isShiftKeyDown()) {
            if (level().isClientSide()) com.simplespace.client.TarsMenuScreen.open(this);
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<TarsEntity> state) {
        if (this.isDigging()) state.setAnimation(DIG);
        else if (state.isMoving()) {
            if (this.isSprintMode()) state.setAnimation(RUN);
            else state.setAnimation(WALK);
        } else state.setAnimation(IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public boolean isPushable() { return false; }
}
