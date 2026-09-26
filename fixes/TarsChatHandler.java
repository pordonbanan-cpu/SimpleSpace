package com.simplespace.event;

import com.simplespace.SimpleSpace;
import com.simplespace.tars.TarsEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

@EventBusSubscriber(modid = SimpleSpace.MOD_ID)
public class TarsChatHandler {

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String msg = event.getMessage().getString();
        if (msg == null || msg.isBlank()) {
            try { msg = event.getRawText(); } catch (Throwable ignored) {}
        }
        if (msg == null || msg.isBlank()) return;

        String lower = msg.toLowerCase().trim();
        boolean addressed = lower.contains("tars") || lower.contains("тарс")
                || lower.startsWith("за мной") || lower.startsWith("стой")
                || lower.startsWith("беги") || lower.startsWith("перекат")
                || lower.startsWith("иди") || lower.contains("иди на")
                || lower.contains("координат");

        if (!addressed) return;

        AABB box = player.getBoundingBox().inflate(64);
        TarsEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : player.level().getEntities(player, box, ent -> ent instanceof TarsEntity)) {
            TarsEntity t = (TarsEntity) e;
            if (t.getOwnerUUID() != null && !t.getOwnerUUID().equals(player.getUUID())) continue;
            double d = t.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = t; }
        }
        if (best == null) return;

        if (best.getOwnerUUID() == null) best.setOwnerUUID(player.getUUID());
        best.handleVoiceCommand(player, msg);
    }
}
