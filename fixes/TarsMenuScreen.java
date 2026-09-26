package com.simplespace.client;

import com.simplespace.tars.TarsEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TarsMenuScreen extends Screen {

    private final TarsEntity tars;
    private static final int PANEL_W = 250;
    private static final int PANEL_H = 248;
    private static final int GOLD = 0xFFC8A030;
    private static final int GOLD_DIM = 0xFF8A7020;
    private static final int GOLD_SOFT = 0x44C8A030;
    private static final int PANEL_BG = 0xF0121218;
    private static final int HEADER_BG = 0xF01A1A22;
    private static final int BTN_BG = 0xFF1C1C24;
    private static final int BTN_HOVER = 0xFF2A2A35;
    private static final int BTN_BORDER = 0xFF3A3A48;

    private EditBox boxX, boxY, boxZ;

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
        int top = this.height / 2 - PANEL_H / 2 + 28;
        int bw = 210, bh = 17, gap = 20;

        addRenderableWidget(goldBtn("▶  Следовать", cx - bw / 2, top, bw, bh, () -> runCmd("tars follow")));
        addRenderableWidget(goldBtn("❚❚  Стоять", cx - bw / 2, top + gap, bw, bh, () -> runCmd("tars stay")));
        addRenderableWidget(goldBtn("⚡  Бежать · перекат", cx - bw / 2, top + gap * 2, bw, bh, () -> runCmd("tars sprint")));

        int half = bw / 2 - 4;
        addRenderableWidget(goldBtn("Юмор 25%", cx - bw / 2, top + gap * 3, half, bh, () -> runCmd("tars humor 25")));
        addRenderableWidget(goldBtn("Юмор 75%", cx + 4, top + gap * 3, half, bh, () -> runCmd("tars humor 75")));

        int fieldW = 56;
        int fy = top + gap * 4 + 1;
        int fx = cx - bw / 2;
        boxX = new EditBox(this.font, fx, fy, fieldW, 15, Component.literal("X"));
        boxY = new EditBox(this.font, fx + fieldW + 6, fy, fieldW, 15, Component.literal("Y"));
        boxZ = new EditBox(this.font, fx + (fieldW + 6) * 2, fy, fieldW, 15, Component.literal("Z"));
        boxX.setMaxLength(9); boxY.setMaxLength(9); boxZ.setMaxLength(9);
        boxX.setHint(Component.literal("X"));
        boxY.setHint(Component.literal("Y"));
        boxZ.setHint(Component.literal("Z"));
        if (minecraft != null && minecraft.player != null) {
            boxX.setValue(String.valueOf((int) Math.floor(minecraft.player.getX())));
            boxY.setValue(String.valueOf((int) Math.floor(minecraft.player.getY())));
            boxZ.setValue(String.valueOf((int) Math.floor(minecraft.player.getZ())));
        }
        addRenderableWidget(boxX);
        addRenderableWidget(boxY);
        addRenderableWidget(boxZ);

        addRenderableWidget(goldBtn("Идти на XYZ", cx - bw / 2, top + gap * 5, bw, bh, this::sendGoTo));
        addRenderableWidget(goldBtn("💾  Перепрошивка", cx - bw / 2, top + gap * 6, bw, bh, () -> {
            if (minecraft != null) minecraft.setScreen(new TarsFlashScreen(tars));
        }));
        addRenderableWidget(goldBtn("Статус", cx - bw / 2, top + gap * 7, half, bh, () -> runCmd("tars status")));
        addRenderableWidget(goldBtn("Закрыть", cx + 4, top + gap * 7, half, bh, this::onClose));
    }

    private void sendGoTo() {
        String x = boxX.getValue().trim(), y = boxY.getValue().trim(), z = boxZ.getValue().trim();
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) return;
        runCmd("tars goto " + x + " " + y + " " + z);
    }

    private GoldButton goldBtn(String label, int x, int y, int w, int h, Runnable action) {
        return new GoldButton(x, y, w, h, Component.literal(label), action);
    }

    private void runCmd(String cmd) {
        if (minecraft != null && minecraft.player != null)
            minecraft.player.connection.sendCommand(cmd);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int cx = width / 2, cy = height / 2;
        int x0 = cx - PANEL_W / 2, y0 = cy - PANEL_H / 2;
        g.fill(x0 - 2, y0 - 2, x0 + PANEL_W + 2, y0 + PANEL_H + 2, GOLD_SOFT);
        g.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, PANEL_BG);
        g.fill(x0, y0, x0 + PANEL_W, y0 + 22, HEADER_BG);
        g.renderOutline(x0, y0, PANEL_W, PANEL_H, GOLD);
        g.fill(x0 + 8, y0 + 21, x0 + PANEL_W - 8, y0 + 22, GOLD_DIM);
        g.drawCenteredString(font, "§6TARS §8· §7панель", cx, y0 + 7, 0xFFFFFF);
        String sub = "§7L" + tars.getFlashLevel() + " §8· §7юмор §e" + tars.getHumor() + "%"
                + (tars.isFollowing() ? " §a●" : " §c●")
                + (tars.isSprintMode() ? " §bперекат" : "");
        g.drawCenteredString(font, sub, cx, y0 + PANEL_H - 11, 0xAAAAAA);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static class GoldButton extends AbstractButton {
        private final Runnable action;
        GoldButton(int x, int y, int w, int h, Component msg, Runnable action) {
            super(x, y, w, h, msg); this.action = action;
        }
        @Override public void onPress() { action.run(); }
        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
            boolean hov = isHoveredOrFocused();
            g.fill(getX(), getY(), getX() + width, getY() + height, hov ? BTN_HOVER : BTN_BG);
            g.renderOutline(getX(), getY(), width, height, hov ? GOLD : BTN_BORDER);
            if (hov) g.fill(getX(), getY(), getX() + 2, getY() + height, GOLD);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, hov ? 0xFFE8C840 : 0xFFDDDDDD);
        }
        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
    }
}
