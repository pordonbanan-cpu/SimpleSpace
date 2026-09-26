package com.simplespace.tars;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");

    public TarsEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
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
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public int getHumor() { return this.entityData.get(HUMOR); }
    public void setHumor(int v) { this.entityData.set(HUMOR, Math.max(0, Math.min(100, v))); }
    public boolean isFollowing() { return this.entityData.get(FOLLOWING); }
    public void setFollowing(boolean v) { this.entityData.set(FOLLOWING, v); }
    public boolean isSprintMode() { return this.entityData.get(SPRINT_MODE); }
    public void setSprintMode(boolean v) { this.entityData.set(SPRINT_MODE, v); }
    public UUID getOwnerUUID() { return ownerUUID; }
    public void setOwnerUUID(UUID uuid) { this.ownerUUID = uuid; }

    public Player getOwner() {
        if (ownerUUID == null || level().isClientSide()) return null;
        return level().getPlayerByUUID(ownerUUID);
    }

    public void speak(ServerPlayer to, String line) {
        String prefix = "§8[TARS] §f";
        if (getHumor() >= 70 && random.nextFloat() < getHumor() / 160f) {
            String[] jokes = {
                    "Юмор " + getHumor() + "%.",
                    "Это была шутка. Или нет.",
                    "Коопера.",
                    "Честность 90%."
            };
            to.sendSystemMessage(Component.literal(prefix + jokes[random.nextInt(jokes.length)]));
        }
        to.sendSystemMessage(Component.literal(prefix + line));
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
            if (this.isSprintMode()) {
                state.setAnimation(RUN);
            } else {
                state.setAnimation(WALK);
            }
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
