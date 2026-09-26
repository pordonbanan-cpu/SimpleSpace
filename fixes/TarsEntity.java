package com.simplespace.tars;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class TarsEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<Integer> HUMOR =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FOLLOWING =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPRINT_MODE =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUUID;
    private int jokeCooldown;
    private int walkSoundCooldown;
    private int rollSoundCooldown;
    private float lockedYRot = Float.NaN;
    private boolean wasMoving;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");

    private static final String[] JOKES = {
            "Это была шутка. Или нет.",
            "Коопера.",
            "Честность 90%. Юмор… пересчитываю.",
            "Я бы пошутил про гравитацию, но она меня не держит.",
            "Ваш план имеет 12% успеха. Вдохновляет.",
            "Модули на месте. Энтузиазм — опционален.",
            "Я не игнорирую приказы. Я приоритизирую.",
            "Если бы у меня было чувство юмора на 100%, вы бы уже смеялись. Или нет."
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
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TarsFollowOwnerGoal(this, 1.25, 1.5f, 2.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !TarsEntity.this.isFollowing() && super.canUse();
            }
        });
    }

    public int getHumor() { return this.entityData.get(HUMOR); }
    public void setHumor(int v) { this.entityData.set(HUMOR, Math.max(0, Math.min(100, v))); }
    public boolean isFollowing() { return this.entityData.get(FOLLOWING); }

    public void setFollowing(boolean v) {
        boolean prev = isFollowing();
        this.entityData.set(FOLLOWING, v);
        if (!v) {
            this.lockedYRot = this.getYRot();
            if (prev && !level().isClientSide()) {
                playLocal(SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.2f);
            }
        } else {
            this.lockedYRot = Float.NaN;
            if (!prev && !level().isClientSide()) {
                playLocal(SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.3f);
            }
        }
    }

    public boolean isSprintMode() { return this.entityData.get(SPRINT_MODE); }

    public void setSprintMode(boolean v) {
        boolean prev = isSprintMode();
        this.entityData.set(SPRINT_MODE, v);
        if (!level().isClientSide() && v != prev) {
            if (v) playLocal(SoundEvents.PISTON_EXTEND, 0.7f, 0.8f);
            else playLocal(SoundEvents.PISTON_CONTRACT, 0.7f, 0.9f);
        }
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

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            orientBody();
            tickMovementSounds();
            tickJokes();
        }
    }

    private void orientBody() {
        if (!isFollowing()) {
            if (!Float.isNaN(lockedYRot)) {
                this.setYRot(lockedYRot);
                this.yBodyRot = lockedYRot;
                this.yHeadRot = lockedYRot;
            }
            return;
        }

        Player owner = getOwner();
        Vec3 motion = this.getDeltaMovement();
        double speed2 = motion.x * motion.x + motion.z * motion.z;
        boolean moving = speed2 > 0.0008 || !this.getNavigation().isDone();

        if (moving) {
            double mx = motion.x;
            double mz = motion.z;
            if (speed2 < 0.0001 && owner != null) {
                mx = owner.getX() - getX();
                mz = owner.getZ() - getZ();
            }
            if (mx * mx + mz * mz > 1.0E-6) {
                float target = (float) (Math.atan2(mz, mx) * (180F / Math.PI)) - 90.0F;
                float maxDelta = isSprintMode() ? 6.0f : 10.0f;
                float next = approachDegrees(this.getYRot(), target, maxDelta);
                this.setYRot(next);
                this.yBodyRot = next;
                this.yHeadRot = next;
            }
        } else if (owner != null && owner.isAlive()) {
            double dx = owner.getX() - getX();
            double dz = owner.getZ() - getZ();
            float target = (float) (Math.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
            float next = approachDegrees(this.getYRot(), target, 4.0f);
            this.setYRot(next);
            this.yBodyRot = next;
            this.yHeadRot = next;
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
        } else if (moving && isFollowing()) {
            if (--walkSoundCooldown <= 0) {
                walkSoundCooldown = 12 + random.nextInt(4);
                playLocal(SoundEvents.IRON_GOLEM_STEP, 0.4f, 1.1f + random.nextFloat() * 0.2f);
            }
        }

        if (wasMoving && !moving && isSprintMode()) {
            playLocal(SoundEvents.IRON_GOLEM_DAMAGE, 0.3f, 1.5f);
        }
        wasMoving = moving;
    }

    private void tickJokes() {
        if (getHumor() <= 0) return;
        if (--jokeCooldown > 0) return;
        float chance = getHumor() / 100f;
        jokeCooldown = 300 + random.nextInt(400);
        if (random.nextFloat() > chance * 0.55f) return;
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
            if (!level().isClientSide() && player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c[TARS] Не ваш робот."));
            }
            return InteractionResult.CONSUME;
        }
        if (ownerUUID == null) setOwnerUUID(player.getUUID());

        if (player.isShiftKeyDown()) {
            if (level().isClientSide()) {
                com.simplespace.client.TarsMenuScreen.open(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }

        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            speak(sp, "Юмор " + getHumor() + "%. Shift+ПКМ — панель.");
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<TarsEntity> state) {
        if (state.isMoving()) {
            if (this.isSprintMode()) state.setAnimation(RUN);
            else state.setAnimation(WALK);
        } else {
            state.setAnimation(IDLE);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public boolean isPushable() { return false; }
}
