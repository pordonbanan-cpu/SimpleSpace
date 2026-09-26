package com.simplespace.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simplespace.entity.ModEntities;
import com.simplespace.tars.TarsEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TarsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("tars").requires(s -> s.hasPermission(0));

        root.then(Commands.literal("призвать").executes(ctx -> spawn(ctx.getSource())));
        root.then(Commands.literal("за_мной").executes(ctx -> setFollow(ctx.getSource(), true)));
        root.then(Commands.literal("стой").executes(ctx -> setFollow(ctx.getSource(), false)));
        root.then(Commands.literal("юмор")
                .then(Commands.argument("уровень", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setHumor(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "уровень")))));
        root.then(Commands.literal("статус").executes(ctx -> status(ctx.getSource())));

        root.then(Commands.literal("spawn").executes(ctx -> spawn(ctx.getSource())));
        root.then(Commands.literal("follow").executes(ctx -> setFollow(ctx.getSource(), true)));
        root.then(Commands.literal("stay").executes(ctx -> setFollow(ctx.getSource(), false)));
        root.then(Commands.literal("humor")
                .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> setHumor(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "level")))));
        root.then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));

        dispatcher.register(root);
    }

    private static TarsEntity findNearest(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(48);
        List<TarsEntity> list = level.getEntitiesOfClass(TarsEntity.class, box,
                t -> player.getUUID().equals(t.getOwnerUUID()));
        TarsEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (TarsEntity t : list) {
            double d = t.distanceToSqr(player);
            if (d < bestD) {
                bestD = d;
                best = t;
            }
        }
        return best;
    }

    private static int spawn(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = player.serverLevel();

        AABB box = player.getBoundingBox().inflate(64);
        for (TarsEntity old : level.getEntitiesOfClass(TarsEntity.class, box,
                t -> player.getUUID().equals(t.getOwnerUUID()))) {
            old.discard();
        }

        TarsEntity tars = ModEntities.TARS.get().create(level);
        if (tars == null) {
            source.sendFailure(Component.literal("Не удалось создать TARS"));
            return 0;
        }
        tars.moveTo(player.getX() + 1.2, player.getY(), player.getZ() + 1.2,
                player.getYRot(), 0);
        tars.setOwnerUUID(player.getUUID());
        tars.setHumor(75);
        tars.setFollowing(true);
        level.addFreshEntity(tars);

        tars.speak(player, "На связи. Юмор 75%. Куда идём?");
        source.sendSuccess(() -> Component.literal("§aTARS призван · Shift+ПКМ — меню"), false);
        return 1;
    }

    private static int setFollow(CommandSourceStack source, boolean follow) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        TarsEntity tars = findNearest(player);
        if (tars == null) {
            source.sendFailure(Component.literal("Рядом нет TARS. /tars spawn"));
            return 0;
        }
        tars.setFollowing(follow);
        tars.speak(player, follow ? "Иду за вами." : "Стоя на месте.");
        return 1;
    }

    private static int setHumor(CommandSourceStack source, int level) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        TarsEntity tars = findNearest(player);
        if (tars == null) {
            source.sendFailure(Component.literal("Рядом нет TARS. /tars spawn"));
            return 0;
        }
        tars.setHumor(level);
        tars.speak(player, "Уровень юмора: " + level + "%.");
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        TarsEntity tars = findNearest(player);
        if (tars == null) {
            source.sendFailure(Component.literal("Рядом нет TARS. /tars spawn"));
            return 0;
        }
        String msg = "§8[TARS] §fЮмор: " + tars.getHumor() + "% | "
                + (tars.isFollowing() ? "следует" : "стоит") + " | HP: "
                + (int) tars.getHealth() + "/" + (int) tars.getMaxHealth();
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
