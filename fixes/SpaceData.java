package com.simplespace.space;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Глобальное серверное хранилище телеметрии космоса.
 * Пишет сенсор в любом измерении, читает блок связи / монитор отовсюду.
 * Хранится в Overworld data storage (одно на весь сервер).
 */
public class SpaceData extends SavedData {

    public static final String ID = "simplespace_space_data";

    private double apoapsis;
    private double periapsis;
    private double inclination;
    private double velocity;
    private double gravity;
    private double posX, posY, posZ;
    private String biome = "unknown";
    private String dimension = "unknown";
    private String body = "none";
    private long timestamp;
    private boolean valid;

    public SpaceData() {}

    public static SpaceData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            // fallback — крайне редко
            return new SpaceData();
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(new Factory<>(SpaceData::new, SpaceData::load), ID);
    }

    public static SpaceData get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static SpaceData load(CompoundTag tag, HolderLookup.Provider registries) {
        SpaceData data = new SpaceData();
        data.apoapsis = tag.getDouble("apoapsis");
        data.periapsis = tag.getDouble("periapsis");
        data.inclination = tag.getDouble("inclination");
        data.velocity = tag.getDouble("velocity");
        data.gravity = tag.getDouble("gravity");
        data.posX = tag.getDouble("posX");
        data.posY = tag.getDouble("posY");
        data.posZ = tag.getDouble("posZ");
        data.biome = tag.contains("biome") ? tag.getString("biome") : "unknown";
        data.dimension = tag.contains("dimension") ? tag.getString("dimension") : "unknown";
        data.body = tag.contains("body") ? tag.getString("body") : "none";
        data.timestamp = tag.getLong("timestamp");
        data.valid = tag.getBoolean("valid");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble("apoapsis", apoapsis);
        tag.putDouble("periapsis", periapsis);
        tag.putDouble("inclination", inclination);
        tag.putDouble("velocity", velocity);
        tag.putDouble("gravity", gravity);
        tag.putDouble("posX", posX);
        tag.putDouble("posY", posY);
        tag.putDouble("posZ", posZ);
        tag.putString("biome", biome != null ? biome : "unknown");
        tag.putString("dimension", dimension != null ? dimension : "unknown");
        tag.putString("body", body != null ? body : "none");
        tag.putLong("timestamp", timestamp);
        tag.putBoolean("valid", valid);
        return tag;
    }

    /** Полная запись с сенсора. */
    public void writeTelemetry(
            double apo, double peri, double incl,
            double vel, double grav,
            double x, double y, double z,
            String biomeId, String dimId, String bodyName,
            long gameTime
    ) {
        this.apoapsis = apo;
        this.periapsis = peri;
        this.inclination = incl;
        this.velocity = vel;
        this.gravity = grav;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.biome = biomeId != null ? biomeId : "unknown";
        this.dimension = dimId != null ? dimId : "unknown";
        this.body = bodyName != null ? bodyName : "none";
        this.timestamp = gameTime;
        this.valid = true;
        setDirty();
    }

    public void clear() {
        valid = false;
        setDirty();
    }

    // —— getters ——

    public double getApoapsis() { return apoapsis; }
    public double getPeriapsis() { return periapsis; }
    public double getInclination() { return inclination; }
    public double getVelocity() { return velocity; }
    public double getGravity() { return gravity; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public double getPosZ() { return posZ; }
    public String getBiome() { return biome; }
    public String getDimension() { return dimension; }
    public String getBody() { return body; }
    public long getTimestamp() { return timestamp; }
    public boolean isValid() { return valid; }

    /** Возраст данных в тиках относительно текущего gameTime. */
    public long ageTicks(long now) {
        return Math.max(0, now - timestamp);
    }

    public String summaryLine() {
        if (!valid) return "NO SIGNAL";
        return String.format("v=%.1f apo=%.0f peri=%.0f i=%.1f° %s",
                velocity, apoapsis, periapsis, inclination, body);
    }
}
