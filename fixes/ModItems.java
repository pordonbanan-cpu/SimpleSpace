package com.simplespace.item;

import com.simplespace.SimpleSpace;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("SimpleSpace"))
                    .icon(() -> new ItemStack(TARS_CORE.get()))
                    .displayItems((params, out) -> {
                        out.accept(TARS_CORE.get());
                        out.accept(FLASH_L1.get());
                        out.accept(FLASH_L2.get());
                        out.accept(FLASH_L3.get());
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
        return 0;
    }
}
