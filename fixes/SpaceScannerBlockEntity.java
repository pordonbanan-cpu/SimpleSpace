package com.simplespace.block.entity;

import com.simplespace.space.SpaceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Космический сенсор: раз в 20 тиков пишет телеметрию в {@link SpaceData}.
 */
public class SpaceScannerBlockEntity extends BlockEntity {

    private int tickCounter;

    public SpaceScannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPACE_SCANNER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpaceScannerBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel server)) return;

        be.tickCounter++;
        if (be.tickCounter < 20) return;
        be.tickCounter = 0;

        be.scanAndWrite(server, pos);
    }

    private void scanAndWrite(ServerLevel level, BlockPos pos) {
        Vec3 at = Vec3.atCenterOf(pos);

        Holder<Biome> biomeHolder = level.getBiome(pos);
        String biomeId = biomeHolder.unwrapKey()
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .orElse("unknown");

        String dimId = level.dimension().location().toString();

        float gravity = level.dimensionType().hasCeiling() ? 0.1f
                : (isSpaceLike(dimId) ? 0.0f : 1.0f);

        double y = at.y;
        double peri = Math.max(0, y - 40);
        double apo = y + 40;
        double incl = 0.0;
        double velocity = 0.0;
        String body = guessBody(dimId, y);

        CosmoSample cosmo = tryCosmonautics(level, pos);
        if (cosmo != null) {
            apo = cosmo.apo;
            peri = cosmo.peri;
            incl = cosmo.incl;
            velocity = cosmo.vel;
            gravity = (float) cosmo.grav;
            if (cosmo.body != null) body = cosmo.body;
        }

        SpaceData data = SpaceData.get(level);
        data.writeTelemetry(
                apo, peri, incl,
                velocity, gravity,
                at.x, at.y, at.z,
                biomeId, dimId, body,
                level.getGameTime()
        );
    }

    private static boolean isSpaceLike(String dimId) {
        String d = dimId.toLowerCase();
        return d.contains("space") || d.contains("orbit") || d.contains("cosmo")
                || d.contains("deep_space") || d.contains("rocketnautics");
    }

    private static String guessBody(String dimId, double y) {
        String d = dimId.toLowerCase();
        if (d.contains("moon")) return "moon";
        if (d.contains("mars")) return "mars";
        if (isSpaceLike(d)) return y > 200 ? "deep_space" : "orbit";
        if (d.contains("overworld")) return "earth";
        return "unknown";
    }

    private static CosmoSample tryCosmonautics(ServerLevel level, BlockPos pos) {
        // Soft-hook под API Cosmonautics — пока null, билд без зависимости
        return null;
    }

    private static final class CosmoSample {
        double apo, peri, incl, vel, grav;
        String body;
    }
}
