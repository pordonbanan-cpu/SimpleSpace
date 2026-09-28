package com.simplespace.block.entity;

import com.simplespace.space.SpaceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Наземный блок связи: читает {@link SpaceData}, кэширует снимок для мониторов.
 * Работает в любом измерении — данные глобальные через Overworld SavedData.
 */
public class GroundCommBlockEntity extends BlockEntity {

    private int tickCounter;

    // Кэш последней телеметрии (для getUpdateTag → клиент / монитор)
    private boolean signalValid;
    private double apoapsis, periapsis, inclination, velocity, gravity;
    private String biome = "—";
    private String body = "—";
    private String dimension = "—";
    private long dataAge;

    public GroundCommBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROUND_COMM.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GroundCommBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel server)) return;

        be.tickCounter++;
        if (be.tickCounter < 20) return;
        be.tickCounter = 0;

        SpaceData data = SpaceData.get(server);
        boolean changed = be.pullFrom(data, server.getGameTime());
        if (changed) {
            be.setChanged();
            server.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private boolean pullFrom(SpaceData data, long now) {
        boolean was = signalValid;
        double a = apoapsis, p = periapsis, i = inclination, v = velocity;

        signalValid = data.isValid();
        if (signalValid) {
            apoapsis = data.getApoapsis();
            periapsis = data.getPeriapsis();
            inclination = data.getInclination();
            velocity = data.getVelocity();
            gravity = data.getGravity();
            biome = data.getBiome();
            body = data.getBody();
            dimension = data.getDimension();
            dataAge = data.ageTicks(now);
        } else {
            dataAge = Long.MAX_VALUE;
        }

        return was != signalValid
                || a != apoapsis || p != periapsis || i != inclination || v != velocity;
    }

    // —— доступ для соседнего монитора (сервер) ——

    public boolean hasSignal() { return signalValid; }
    public double getApoapsis() { return apoapsis; }
    public double getPeriapsis() { return periapsis; }
    public double getInclination() { return inclination; }
    public double getVelocity() { return velocity; }
    public double getGravity() { return gravity; }
    public String getBiome() { return biome; }
    public String getBody() { return body; }
    public String getDimension() { return dimension; }
    public long getDataAge() { return dataAge; }

    public String displaySummary() {
        if (!signalValid) return "NO SIGNAL";
        return String.format("BODY %s  V %.1f\nAPO %.0f  PERI %.0f\nINC %.1f  G %.2f",
                body, velocity, apoapsis, periapsis, inclination, gravity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("valid", signalValid);
        tag.putDouble("apo", apoapsis);
        tag.putDouble("peri", periapsis);
        tag.putDouble("inc", inclination);
        tag.putDouble("vel", velocity);
        tag.putDouble("grav", gravity);
        tag.putString("biome", biome);
        tag.putString("body", body);
        tag.putString("dim", dimension);
        tag.putLong("age", dataAge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signalValid = tag.getBoolean("valid");
        apoapsis = tag.getDouble("apo");
        periapsis = tag.getDouble("peri");
        inclination = tag.getDouble("inc");
        velocity = tag.getDouble("vel");
        gravity = tag.getDouble("grav");
        biome = tag.contains("biome") ? tag.getString("biome") : "—";
        body = tag.contains("body") ? tag.getString("body") : "—";
        dimension = tag.contains("dim") ? tag.getString("dim") : "—";
        dataAge = tag.getLong("age");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
