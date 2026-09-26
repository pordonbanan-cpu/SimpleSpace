package com.simplespace.client;

import com.simplespace.item.ModItems;
import com.simplespace.tars.TarsEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TarsFlashScreen extends Screen {

    private final TarsEntity tars;
    private static final int PANEL_W = 230;
    private static final int PANEL_H = 160;
    private static final int GOLD = 0xFFC8A030;
    private static final int PANEL_BG = 0xF0121218;

    public TarsFlashScreen(TarsEntity tars) {
        super(Component.literal("Перепрошивка"));
        this.tars = tars;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        int top = cy - PANEL_H / 2 + 50;

        addRenderableWidget(new Btn(cx - 90, top, 180, 20, Component.literal("Установить флешку"), () -> {
            if (minecraft != null && minecraft.player != null) {
                int tier = findFlashInInv();
                if (tier > 0) minecraft.player.connection.sendCommand("tars flash " + tier);
            }
        }));
        addRenderableWidget(new Btn(cx - 90, top + 28, 180, 20, Component.literal("Назад"),
                () -> { if (minecraft != null) minecraft.setScreen(new TarsMenuScreen(tars)); }));
    }

    private int findFlashInInv() {
        if (minecraft == null || minecraft.player == null) return 0;
        int hand = ModItems.getFlashTier(minecraft.player.getMainHandItem());
        if (hand > 0) return hand;
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            int t = ModItems.getFlashTier(inv.getItem(i));
            if (t > 0) return t;
        }
        return 0;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int cx = width / 2, cy = height / 2;
        int x0 = cx - PANEL_W / 2, y0 = cy - PANEL_H / 2;
        g.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, PANEL_BG);
        g.renderOutline(x0, y0, PANEL_W, PANEL_H, GOLD);
        g.drawCenteredString(font, "§6Перепрошивка TARS", cx, y0 + 10, 0xFFFFFF);
        g.drawCenteredString(font, "§7Текущий модуль: §eL" + tars.getFlashLevel(), cx, y0 + 26, 0xAAAAAA);
        g.drawCenteredString(font, "§8L1 сканер · L2 добыча · L3 быстрая добыча", cx, y0 + 40, 0x888888);

        int prog = tars.getFlashProgress();
        int max = TarsEntity.FLASH_INSTALL_TICKS;
        int barW = 180, barH = 10;
        int bx = cx - barW / 2, by = y0 + 100;
        g.fill(bx, by, bx + barW, by + barH, 0xFF222228);
        if (prog > 0) {
            int fill = (int) (barW * (prog / (float) max));
            g.fill(bx, by, bx + fill, by + barH, GOLD);
            g.drawCenteredString(font, "§eУстановка… " + (prog * 100 / max) + "%", cx, by - 12, 0xFFFFFF);
        } else {
            g.drawCenteredString(font, "§7Флешка в руке или инвентаре", cx, by - 12, 0xAAAAAA);
        }
        g.renderOutline(bx, by, barW, barH, 0xFF444450);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static class Btn extends AbstractButton {
        private final Runnable action;
        Btn(int x, int y, int w, int h, Component msg, Runnable a) {
            super(x, y, w, h, msg); this.action = a;
        }
        @Override public void onPress() { action.run(); }
        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float p) {
            boolean h = isHoveredOrFocused();
            g.fill(getX(), getY(), getX() + width, getY() + height, h ? 0xFF2A2A35 : 0xFF1C1C24);
            g.renderOutline(getX(), getY(), width, height, h ? GOLD : 0xFF3A3A48);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, h ? 0xFFE8C840 : 0xFFDDDDDD);
        }
        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
    }
}
