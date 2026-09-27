package com.simplespace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.simplespace.item.ModItems;
import com.simplespace.tars.TarsEntity;
import com.simplespace.entity.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class TarsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("tars")
                .then(Commands.literal("spawn").executes(ctx -> spawn(ctx.getSource())))
                .then(Commands.literal("follow").executes(ctx -> order(ctx.getSource(), "follow")))
                .then(Commands.literal("stay").executes(ctx -> order(ctx.getSource(), "stay")))
                .then(Commands.literal("sprint").executes(ctx -> order(ctx.getSource(), "sprint")))
                .then(Commands.literal("status").executes(ctx -> order(ctx.getSource(), "status")))
                .then(Commands.literal("give").executes(ctx -> give(ctx.getSource())))
                .then(Commands.literal("collect")
                    .then(Commands.argument("mode", StringArgumentType.word())
                        .executes(ctx -> collect(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                .then(Commands.literal("humor")
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> humor(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "value")))))
                .then(Commands.literal("goto")
                    .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                            .then(Commands.argument("z", IntegerArgumentType.integer())
                                .executes(ctx -> gotoCmd(ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "x"),
                                    IntegerArgumentType.getInteger(ctx, "y"),
                                    IntegerArgumentType.getInteger(ctx, "z")))))))
                .then(Commands.literal("flash")
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 3))
                        .executes(ctx -> flash(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("за_мной").executes(ctx -> order(ctx.getSource(), "follow")))
                .then(Commands.literal("стой").executes(ctx -> order(ctx.getSource(), "stay")))
                .then(Commands.literal("беги").executes(ctx -> order(ctx.getSource(), "sprint")))
        );
    }

    private static TarsEntity nearest(ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(64);
        TarsEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : p.level().getEntities(p, box, ent -> ent instanceof TarsEntity)) {
            TarsEntity t = (TarsEntity) e;
            if (t.getOwnerUUID() != null && !t.getOwnerUUID().equals(p.getUUID())) continue;
            double d = t.distanceToSqr(p);
            if (d < bestD) { bestD = d; best = t; }
        }
        return best;
    }

    private static int spawn(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        ServerLevel level = p.serverLevel();
        TarsEntity t = ModEntities.TARS.get().create(level);
        if (t == null) return 0;
        t.moveTo(p.getX() + 1.5, p.getY(), p.getZ() + 1.5, p.getYRot(), 0);
        t.setOwnerUUID(p.getUUID());
        level.addFreshEntity(t);
        t.speak(p, "Онлайн. Shift+ПКМ — панель.");
        return 1;
    }

    private static int order(CommandSourceStack src, String kind) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        TarsEntity t = nearest(p);
        if (t == null) {
            src.sendFailure(Component.literal("TARS не найден рядом."));
            return 0;
        }
        if (t.getOwnerUUID() == null) t.setOwnerUUID(p.getUUID());
        switch (kind) {
            case "follow" -> {
                t.setMiningOrdered(false);
                t.setFollowing(true); t.setSprintMode(false); t.clearGoToTarget(false);
                t.speak(p, "Иду за вами.");
            }
            case "stay" -> {
                t.setMiningOrdered(false);
                t.setFollowing(false); t.setSprintMode(false); t.clearGoToTarget(false);
                t.getNavigation().stop(); t.speak(p, "Стою.");
            }
            case "sprint" -> {
                t.setMiningOrdered(false);
                t.setFollowing(true); t.setSprintMode(true); t.clearGoToTarget(false);
                t.speak(p, "Перекат.");
            }
            case "status" -> t.speak(p, "L" + t.getFlashLevel() + ". Юмор " + t.getHumor() + "%. "
                    + (t.isFollowing() ? "Следую." : "Стою.")
                    + (t.isSprintMode() ? " Перекат." : "")
                    + " Склад: " + t.countItems() + " шт."
                    + (t.isCollectDrops() ? " Сбор вкл." : " Сбор выкл."));
        }
        return 1;
    }

    private static int give(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        TarsEntity t = nearest(p);
        if (t == null) { src.sendFailure(Component.literal("TARS не найден рядом.")); return 0; }
        int n = t.giveToPlayer(p, false);
        if (n <= 0) t.speak(p, "Склад пуст.");
        else t.speak(p, "Перенёс " + n + " шт.");
        return 1;
    }

    private static int collect(CommandSourceStack src, String mode) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        TarsEntity t = nearest(p);
        if (t == null) { src.sendFailure(Component.literal("TARS не найден рядом.")); return 0; }
        boolean on = mode.equalsIgnoreCase("on") || mode.equalsIgnoreCase("true") || mode.equals("1");
        t.setCollectDrops(on);
        t.speak(p, "Сбор в склад: " + (on ? "вкл" : "выкл") + ".");
        return 1;
    }

    private static int humor(CommandSourceStack src, int value) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        TarsEntity t = nearest(p);
        if (t == null) { src.sendFailure(Component.literal("TARS не найден рядом.")); return 0; }
        t.setHumor(value);
        t.speak(p, "Юмор: " + value + "%.");
        return 1;
    }

    private static int gotoCmd(CommandSourceStack src, int x, int y, int z) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        TarsEntity t = nearest(p);
        if (t == null) { src.sendFailure(Component.literal("TARS не найден рядом.")); return 0; }
        if (t.getOwnerUUID() == null) t.setOwnerUUID(p.getUUID());
        t.setGoToTarget(new BlockPos(x, y, z));
        t.speak(p, "Иду на " + x + " " + y + " " + z + ".");
        return 1;
    }

    private static int flash(CommandSourceStack src, int level) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        TarsEntity t = nearest(p);
        if (t == null) { src.sendFailure(Component.literal("TARS не найден рядом.")); return 0; }
        boolean found = false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (ModItems.getFlashTier(st) == level) {
                if (!p.getAbilities().instabuild) st.shrink(1);
                found = true;
                break;
            }
        }
        if (!found) {
            t.speak(p, "Флешка L" + level + " не найдена в инвентаре.");
            return 0;
        }
        t.beginFlashInstall(level, p);
        return 1;
    }
}
