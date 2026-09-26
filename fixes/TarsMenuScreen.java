package com.simplespace.client;

import com.simplespace.tars.TarsEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TarsMenuScreen extends Screen {

    private final TarsEntity tars;
    private static final int PANEL_W = 236;
    private static final int PANEL_H = 198;

    private static final int GOLD       = 0xFFC8A030;
    private static final int GOLD_DIM   = 0xFF8A7020;
    private static final int GOLD_SOFT  = 0x44C8A030;
    private static final int PANEL_BG   = 0xF0121218;
    private static final int HEADER_BG  = 0xF01A1A22;
    private static final int BTN_BG     = 0xFF1C1C24;
    private static final int BTN_HOVER  = 0xFF2A2A35;
    private static final int BTN_BORDER = 0xFF3A3A48;

    public TarsMenuScreen(TarsEntity tars) {
        super(Component.literal("TARS"));
        this.tars = tars;
    }

    public static void open(TarsEntity tars) {
        Minecraft.getInstance().setScreen(new TarsMenuScreen(tars));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = this.height / 2 - PANEL_H / 2 + 32;
        int bw = 196;
        int bh = 22;
        int gap = 26;

        addRenderableWidget(goldBtn("▶  Следовать", cx - bw / 2, top, bw, bh,
                () -> runCmd("tars follow")));
        addRenderableWidget(goldBtn("❚❚  Стоять", cx - bw / 2, top + gap, bw, bh,
                () -> runCmd("tars stay")));
        addRenderableWidget(goldBtn("⚡  Бежать · перекат", cx - bw / 2, top + gap * 2, bw, bh,
                () -> runCmd("tars sprint")));

        int half = bw / 2 - 4;
        addRenderableWidget(goldBtn("Юмор 25%", cx - bw / 2, top + gap * 3, half, bh,
                () -> runCmd("tars humor 25")));
        addRenderableWidget(goldBtn("Юмор 75%", cx + 4, top + gap * 3, half, bh,
                () -> runCmd("tars humor 75")));
        addRenderableWidget(goldBtn("Юмор 100%", cx - bw / 2, top + gap * 4, bw, bh,
                () -> runCmd("tars humor 100")));

        addRenderableWidget(goldBtn("Статус", cx - bw / 2, top + gap * 5, half, bh,
                () -> runCmd("tars status")));
        addRenderableWidget(goldBtn("Закрыть", cx + 4, top + gap * 5, half, bh,
                this::onClose));
    }

    private GoldButton goldBtn(String label, int x, int y, int w, int h, Runnable action) {
        return new GoldButton(x, y, w, h, Component.literal(label), action);
    }

    private void runCmd(String cmd) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.connection.sendCommand(cmd);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, this.width, this.height, 0x66000000);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int x0 = cx - PANEL_W / 2;
        int y0 = cy - PANEL_H / 2;

        g.fill(x0 - 2, y0 - 2, x0 + PANEL_W + 2, y0 + PANEL_H + 2, GOLD_SOFT);
        g.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, PANEL_BG);
        g.fill(x0, y0, x0 + PANEL_W, y0 + 26, HEADER_BG);
        g.renderOutline(x0, y0, PANEL_W, PANEL_H, GOLD);
        g.renderOutline(x0 + 2, y0 + 2, PANEL_W - 4, PANEL_H - 4, 0x33C8A030);
        g.fill(x0 + 8, y0 + 25, x0 + PANEL_W - 8, y0 + 26, GOLD_DIM);

        g.drawCenteredString(this.font, "§6TARS §8· §7панель", cx, y0 + 9, 0xFFFFFF);

        String sub = "§7Юмор §e" + tars.getHumor() + "%§7  ·  "
                + (tars.isFollowing() ? "§aследует" : "§cстоит")
                + (tars.isSprintMode() ? "  ·  §bбег" : "");
        g.drawCenteredString(this.font, sub, cx, y0 + PANEL_H - 14, 0xAAAAAA);

        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class GoldButton extends AbstractButton {
        private final Runnable action;

        GoldButton(int x, int y, int w, int h, Component msg, Runnable action) {
            super(x, y, w, h, msg);
            this.action = action;
        }

        @Override
        public void onPress() {
            this.action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            boolean hov = this.isHoveredOrFocused();
            int bg = hov ? BTN_HOVER : BTN_BG;
            int border = hov ? GOLD : BTN_BORDER;

            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            g.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            if (hov) {
                g.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.height, GOLD);
            }

            int textColor = hov ? 0xFFE8C840 : 0xFFDDDDDD;
            int ty = this.getY() + (this.height - 8) / 2;
            g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    this.getX() + this.width / 2, ty, textColor);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            this.defaultButtonNarrationText(out);
        }
    }
}
