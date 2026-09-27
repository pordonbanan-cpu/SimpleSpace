package com.simplespace.tars;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.fml.ModList;

/**
 * Мягкая интеграция с Create / Aeronautics без жёсткой зависимости.
 * Create: выгрузка склада TARS в ближайшие контейнеры (сундук/воронка/depot через Container).
 * Aeronautics: заготовка под пилотирование — пока ответ «в разработке».
 */
public final class TarsCreateBridge {

    private TarsCreateBridge() {}

    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded("create");
    }

    public static boolean isAeronauticsLoaded() {
        return ModList.get().isLoaded("aeronautics") || ModList.get().isLoaded("aeronautics_bundled");
    }

    /** Выгрузить инвентарь TARS в ближайшие Container в радиусе. */
    public static int unloadToNearby(TarsEntity tars, int radius) {
        if (tars.level().isClientSide()) return 0;
        Level level = tars.level();
        BlockPos origin = tars.blockPosition();
        int moved = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be == null) continue;
                    Container inv = containerFrom(be);
                    if (inv == null) continue;
                    moved += dumpInto(tars, inv);
                    if (tars.countItems() <= 0) return moved;
                }
            }
        }
        return moved;
    }

    private static Container containerFrom(BlockEntity be) {
        if (be instanceof Container c) return c;
        // Hopper API helper works for many vanilla-like inventories
        try {
            return HopperBlockEntity.getContainerAt(be.getLevel(), be.getBlockPos());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int dumpInto(TarsEntity tars, Container inv) {
        int moved = 0;
        for (int i = 0; i < TarsEntity.INV_SIZE; i++) {
            ItemStack stack = tars.inventoryGet(i);
            if (stack.isEmpty()) continue;
            ItemStack remain = insertStack(inv, stack);
            int put = stack.getCount() - remain.getCount();
            if (put > 0) {
                moved += put;
                tars.inventorySet(i, remain);
            }
        }
        inv.setChanged();
        return moved;
    }

    private static ItemStack insertStack(Container inv, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int s = 0; s < inv.getContainerSize() && !remaining.isEmpty(); s++) {
            if (!inv.canPlaceItem(s, remaining)) continue;
            ItemStack slot = inv.getItem(s);
            if (slot.isEmpty()) {
                int n = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                inv.setItem(s, remaining.copyWithCount(n));
                remaining.shrink(n);
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)
                    && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int n = Math.min(space, remaining.getCount());
                slot.grow(n);
                remaining.shrink(n);
            }
        }
        return remaining;
    }

    public static void handleVoice(TarsEntity tars, ServerPlayer sp, String msg) {
        String m = msg.toLowerCase();

        if (contains(m, "разгруз", "выгруз", "unload", "в сундук", "на склад")) {
            int n = unloadToNearby(tars, 4);
            if (n <= 0) tars.speak(sp, "Рядом нет свободного контейнера.");
            else tars.speak(sp, "Выгрузил " + n + " шт. в контейнеры."
                    + (isCreateLoaded() ? " (Create/сундуки)" : ""));
            return;
        }

        if (contains(m, "конвеер", "конвейер", "depot", "воронк", "create")) {
            if (!isCreateLoaded()) {
                tars.speak(sp, "Create не установлен.");
                return;
            }
            int n = unloadToNearby(tars, 5);
            tars.speak(sp, n > 0
                    ? "Передал " + n + " на ближайшие приёмники Create/контейнеры."
                    : "Приёмник Create рядом не найден — подойдите ближе к depot/воронке.");
            return;
        }

        if (contains(m, "дирижабл", "аэронавт", "aeronaut", "борт", "пилот")) {
            if (!isAeronauticsLoaded()) {
                tars.speak(sp, "Aeronautics не найден. Модуль пилотирования — заготовка.");
            } else {
                tars.speak(sp, "Aeronautics: пилотирование и стыковка — в разработке. Порт готов.");
            }
        }
    }

    private static boolean contains(String m, String... keys) {
        for (String k : keys) if (m.contains(k)) return true;
        return false;
    }
}
