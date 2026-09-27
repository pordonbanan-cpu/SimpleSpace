package com.simplespace.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.component.Filterable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;

import java.util.List;

public class TarsManualItem extends Item {

    public TarsManualItem(Properties props) {
        super(props);
    }

    public static ItemStack createBookStack() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = List.of(
                page("§6TARS — пособие\n§8SimpleSpace\n\n§0Компаньон-робот из Interstellar.\n\n§0Спавн: монолит TARS (крафт) — ПКМ по земле.\n\n§0Панель: §6Shift+ПКМ§0 по TARS."),
                page("§6Голос (в чат)\n\n§0§otarс за мной§r — следовать\n§0§otarс стой§r — стоять\n§0§otarс беги§r — перекат\n§0§otarс док§r / §oпорт§r — в порт\n§0§otarс выйди§r — из порта\n\n§0Координаты:§r\n§otarс иди 100 64 -20§r"),
                page("§6Память мест\n\n§otarс запомни база§r\n§otarс иди на база§r\n§otarс список мест§r\n\n§6Склад\n§otarс отдай§r — всё игроку\n§otarс отдай руду§r\n§otarс склад§r — сколько внутри\n§otarс подбирай§r / §oне подбирай§r"),
                page("§6Добыча (L2+)\n\n§otarс добудь железо§r\n§otarс добудь алмаз§r\n§otarс хватит копать§r\n\n§0Также: уголь, золото, медь, редстоун, лазурит, изумруд, кварц, незерит.\n\n§6Скан (L4+)\n§otarс скан§r / §oчто вокруг§r\n§otarс что часто§r — статистика руд"),
                page("§6Флешки\n\n§0L1 — сканер руд\n§0L2 — добыча\n§0L3 — быстрая добыча\n§0L4 — скан окружения + статистика\n§0L5 — броня (меньше урона)\n\n§0Крафт прогрессивный: для L2 нужна L1 и т.д.\n\n§0Установка: флешка в руке, ПКМ по TARS или GUI «Перепрошивка»."),
                page("§6Порт TARS\n\n§0Крафт: железо + железный блок.\nПоставьте площадку.\n\n§0В GUI: §6⬡ В порт§0 / §6⏏ Из порта§0\nили голос: §oдок§r / §oвыйди§r\n\n§0TARS подходит и ложится на порт (отстой)."),
                page("§6Create / Aeronautics\n\n§otarс разгрузи§r — в сундуки рядом\n§otarс конвейер§r — то же (Create)\n§otarс борт§r — заготовка Aeronautics\n\n§6Команды /tars\n§0follow, stay, sprint, status\n§0goto X Y Z, dock, undock\n§0give, collect on|off\n§0flash 1-5, humor 0-100"),
                page("§6Советы\n\n§0• Сначала обходит препятствия, ломает только если застрял.\n§0• Не копает блок под ногами без нужды.\n§0• На телефоне: меньше RAM (4–5 ГБ), не спавнить сразу после входа.\n\n§8«Это была шутка.» — TARS")
        );
        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough("TARS — пособие"),
                "SimpleSpace",
                0,
                pages,
                true
        );
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            ItemStack book = createBookStack();
            sp.openItemGui(book, hand);
            // openItemGui may need the book in hand — give temporary open via setItem
            ItemStack old = stack.copy();
            player.setItemInHand(hand, book);
            sp.openItemGui(book, hand);
            // keep manual as the item; book UI uses content from stack
            // Actually for WRITTEN_BOOK the stack itself should be the book.
            // Convert manual to book on first use if preferred:
            return InteractionResultHolder.sidedSuccess(book, level.isClientSide());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
