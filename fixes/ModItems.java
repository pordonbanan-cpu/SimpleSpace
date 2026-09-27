package com.simplespace.item;

import com.simplespace.SimpleSpace;
import com.simplespace.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleSpace.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleSpace.MOD_ID);

    public static final DeferredHolder<Item, Item> TARS_CORE =
            ITEMS.register("tars_core", () -> new TarsCoreItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> FLASH_L1 =
            ITEMS.register("flash_l1", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> FLASH_L2 =
            ITEMS.register("flash_l2", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> FLASH_L3 =
            ITEMS.register("flash_l3", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> FLASH_L4 =
            ITEMS.register("flash_l4", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> FLASH_L5 =
            ITEMS.register("flash_l5", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> TARS_DOCK =
            ITEMS.register("tars_dock", () -> new BlockItem(ModBlocks.TARS_DOCK.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> TARS_MANUAL =
            ITEMS.register("tars_manual", () -> new TarsManualItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("SimpleSpace"))
                    .icon(() -> new ItemStack(TARS_CORE.get()))
                    .displayItems((params, out) -> {
                        out.accept(TARS_CORE.get());
                        out.accept(TARS_MANUAL.get());
                        out.accept(FLASH_L1.get());
                        out.accept(FLASH_L2.get());
                        out.accept(FLASH_L3.get());
                        out.accept(FLASH_L4.get());
                        out.accept(FLASH_L5.get());
                        out.accept(TARS_DOCK.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    public static int getFlashTier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Item i = stack.getItem();
        if (i == FLASH_L1.get()) return 1;
        if (i == FLASH_L2.get()) return 2;
        if (i == FLASH_L3.get()) return 3;
        if (i == FLASH_L4.get()) return 4;
        if (i == FLASH_L5.get()) return 5;
        return 0;
    }
}
