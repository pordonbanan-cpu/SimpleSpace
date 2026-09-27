package com.simplespace.item;

import com.simplespace.entity.ModEntities;
import com.simplespace.tars.TarsEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TarsCoreItem extends Item {

    public TarsCoreItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        ServerLevel sl = (ServerLevel) level;
        Vec3 look = player.getLookAngle();
        BlockPos spawnAt = BlockPos.containing(
                player.getX() + look.x * 2.0,
                player.getY(),
                player.getZ() + look.z * 2.0
        );

        TarsEntity tars = ModEntities.TARS.get().create(sl);
        if (tars == null) {
            return InteractionResultHolder.fail(stack);
        }

        tars.moveTo(spawnAt.getX() + 0.5, player.getY(), spawnAt.getZ() + 0.5, player.getYRot(), 0);
        tars.setOwnerUUID(player.getUUID());
        sl.addFreshEntity(tars);

        sl.playSound(null, spawnAt, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.3f);
        sl.playSound(null, spawnAt, SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 0.6f, 0.8f);

        if (player instanceof ServerPlayer sp) {
            tars.speak(sp, "Системы онлайн. Shift+ПКМ — панель.");
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7Монолит TARS"));
        tip.add(Component.literal("§8ПКМ — активировать"));
        tip.add(Component.literal("§8«Это был приказ.»"));
    }
}
