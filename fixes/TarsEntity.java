package com.simplespace.tars;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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

import java.util.UUID;

public class TarsEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> HUMOR =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FOLLOWING =
            SynchedEntityData.defineId(TarsEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerUUID;

    public TarsEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUMOR, 75);
        builder.define(FOLLOWING, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TarsFollowOwnerGoal(this, 1.25, 1.8f, 2.5f));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public int getHumor() {
        return this.entityData.get(HUMOR);
    }

    public void setHumor(int value) {
        this.entityData.set(HUMOR, Math.max(0, Math.min(100, value)));
    }

    public boolean isFollowing() {
        return this.entityData.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.entityData.set(FOLLOWING, following);
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    public Player getOwner() {
        if (ownerUUID == null || level().isClientSide()) return null;
        return level().getPlayerByUUID(ownerUUID);
    }

    public float getWalkAnimSpeed() {
        return this.walkAnimation.speed();
    }

    public float getWalkAnimPos(float partial) {
        return this.walkAnimation.position(partial);
    }

    public void speak(ServerPlayer to, String baseLine) {
        String prefix = "§8[TARS] §f";
        if (getHumor() >= 70 && random.nextFloat() < getHumor() / 150f) {
            String[] jokes = {
                    "Юмор на " + getHumor() + "%. Как и просили.",
                    "Это была шутка. Или нет.",
                    "Коопера, я же робот. Почти.",
                    "Честность 90%. Остальное — стиль."
            };
            to.sendSystemMessage(Component.literal(prefix + jokes[random.nextInt(jokes.length)]));
        }
        to.sendSystemMessage(Component.literal(prefix + baseLine));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (ownerUUID != null && !ownerUUID.equals(player.getUUID())) {
            sp.sendSystemMessage(Component.literal("§c[TARS] Это не ваш робот."));
            return InteractionResult.CONSUME;
        }
        if (ownerUUID == null) {
            setOwnerUUID(player.getUUID());
        }

        if (player.isShiftKeyDown()) {
            openMenu(sp);
            return InteractionResult.CONSUME;
        }

        speak(sp, "Юмор " + getHumor() + "%. "
                + (isFollowing() ? "Следую за вами." : "Стоя. Shift+ПКМ — меню."));
        return InteractionResult.CONSUME;
    }

    private void openMenu(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§6§l—— TARS · меню ——"));
        player.sendSystemMessage(btn("§a▶ За мной", "/tars follow", "Следовать"));
        player.sendSystemMessage(btn("§e❚❚ Стоять", "/tars stay", "Остановиться"));
        player.sendSystemMessage(btn("§bЮмор 25%", "/tars humor 25", "Мало шуток"));
        player.sendSystemMessage(btn("§bЮмор 75%", "/tars humor 75", "Как в фильме"));
        player.sendSystemMessage(btn("§bЮмор 100%", "/tars humor 100", "Максимум"));
        player.sendSystemMessage(btn("§7Статус", "/tars status", "Статус"));
        player.sendSystemMessage(Component.literal("§8Shift+ПКМ по TARS — меню"));
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.literal(label).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
