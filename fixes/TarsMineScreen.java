package com.simplespace.client;

import com.simplespace.tars.TarsEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TarsMineScreen extends Screen {

    private final TarsEntity tars;
    private static final int PANEL_W = 240;
    private static final int PANEL_H = 230;
    private static final int GOLD = 0xFFC8A030;
    private static final int PANEL_BG = 0xF0121218;

    public TarsMineScreen(TarsEntity tars) {
        super(Component.literal("Добыча"));
        this.tars = tars;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = height / 2 - PANEL_H / 2 + 40;
        int bw = 200, bh = 16, gap = 17;

        String collectLabel = tars.isCollectDrops()
                ? "§a☑ Сбор в склад TARS"
                : "§7☐ Сбор в склад (сейчас на землю)";
        addRenderableWidget(btn(collectLabel, cx - bw / 2, top, bw, bh, () -> {
            if (minecraft != null && minecraft.player != null) {
                boolean next = !tars.isCollectDrops();
                minecraft.player.connection.sendCommand("tars collect " + (next ? "on" : "off"));
                tars.setCollectDrops(next);
                minecraft.setScreen(new TarsMineScreen(tars));
            }
        }));

        addRenderableWidget(btn("Добывать любые руды", cx - bw / 2, top + gap, bw, bh,
                () -> run("добудь руды")));
        addRenderableWidget(btn("Добывать железо", cx - bw / 2, top + gap * 2, bw, bh,
                () -> run("добудь железо")));
        addRenderableWidget(btn("Добывать уголь", cx - bw / 2, top + gap * 3, bw, bh,
                () -> run("добудь уголь")));
        addRenderableWidget(btn("Добывать алмазы", cx - bw / 2, top + gap * 4, bw, bh,
                () -> run("добудь алмаз")));
        addRenderableWidget(btn("Где ближайшая руда", cx - bw / 2, top + gap * 5, bw, bh,
                () -> run("где ближайшая руда")));
        addRenderableWidget(btn("Отдай мне склад", cx - bw / 2, top + gap * 6, bw, bh,
                () -> run("отдай")));
        addRenderableWidget(btn("Стоп добыча", cx - bw / 2, top + gap * 7, bw, bh,
                () -> run("хватит копать")));
        addRenderableWidget(btn("Назад", cx - bw / 2, top + gap * 8, bw, bh,
                () -> { if (minecraft != null) minecraft.setScreen(new TarsMenuScreen(tars)); }));
    }

    private void run(String phrase) {
        if (minecraft == null || minecraft.player == null) return;
        minecraft.player.connection.sendChat(phrase);
    }

    private Btn btn(String label, int x, int y, int w, int h, Runnable a) {
        return new Btn(x, y, w, h, Component.literal(label), a);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int cx = width / 2, cy = height / 2;
        int x0 = cx - PANEL_W / 2, y0 = cy - PANEL_H / 2;
        g.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, PANEL_BG);
        g.renderOutline(x0, y0, PANEL_W, PANEL_H, GOLD);
        g.drawCenteredString(font, "§6Добыча §8· §7L" + tars.getFlashLevel(), cx, y0 + 12, 0xFFFFFF);
        g.drawCenteredString(font, "§8L2+ · галочка = в склад TARS", cx, y0 + 24, 0x888888);
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
