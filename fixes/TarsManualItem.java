package com.simplespace.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

import java.util.List;

public class TarsManualItem extends Item {

    public TarsManualItem(Properties props) {
        super(props);
    }

    public static ItemStack createBookStack() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = List.of(
                page("§6TARS — пособие\n§8SimpleSpace\n\n§0Компаньон-робот.\n\n§0Спавн: предмет «Монолит TARS» — ПКМ по земле.\n\n§0Панель: §6Shift+ПКМ§0 по TARS."),
                page("§6Голос (чат)\n\n§otarс за мной§r — следовать\n§otarс стой§r — стоять\n§otarс беги§r — перекат\n§otarс док§r / порт — в порт\n§otarс выйди§r — из порта\n\n§0Координаты:\n§otarс иди 100 64 -20§r"),
                page("§6Память мест\n\n§otarс запомни база§r\n§otarс иди на база§r\n§otarс список мест§r\n\n§6Склад\n§otarс отдай§r\n§otarс отдай руду§r\n§otarс склад§r\n§otarс подбирай§r / не подбирай"),
                page("§6Добыча (L2+)\n\n§otarс добудь железо§r\n§otarс добудь алмаз§r\n§otarс хватит копать§r\n\n§0Руда: уголь, золото, медь, редстоун, лазурит, изумруд, кварц, незерит.\n\n§6Скан (L4+)\n§otarс скан§r\n§otarс что часто§r"),
                page("§6Флешки\n\nL1 — сканер руд\nL2 — добыча\nL3 — быстрая добыча\nL4 — скан + статистика\nL5 — броня\n\n§0Крафт: L2 нужен L1 и т.д.\nУстановка: флешка ПКМ по TARS или GUI."),
                page("§6Порт TARS\n\n§0Крафт: железо + железный блок.\n\n§0В GUI: §6⬡ В порт§0 / §6⏏ Из порта\n§0Голос: док / выйди\n§0Команда: /tars dock\n\n§0TARS ложится на площадку."),
                page("§6Create\n\n§otarс разгрузи§r — в сундуки\n§otarс конвейер§r\n\n§6Aeronautics\n§otarс борт§r — заготовка\n\n§6/tars\n§0follow stay sprint status\n§0goto dock undock give\n§0flash 1-5 humor"),
                page("§6Советы\n\n§0• Обходит, ломает если застрял.\n§0• Не копает пол без нужды.\n§0• На телефоне: RAM 4–5 ГБ.\n\n§8«Это была шутка.»")
        );
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("TARS — пособие"),
                "SimpleSpace",
                0,
                pages,
                true
        ));
        return book;
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            ItemStack book = createBookStack();
            player.setItemInHand(hand, book);
            sp.openItemGui(book, hand);
            return InteractionResultHolder.sidedSuccess(book, level.isClientSide());
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }
}
