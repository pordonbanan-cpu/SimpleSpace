package com.simplespace.client;

import com.simplespace.tars.TarsEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TarsMenuScreen extends Screen {

    private final TarsEntity tars;
    private static final int PANEL_W = 220;
    private static final int PANEL_H = 180;

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
        int bw = 180;
        int bh = 20;
        int gap = 24;

        addRenderableWidget(Button.builder(Component.literal("▶  Следовать"), b -> runCmd("tars follow"))
                .bounds(cx - bw / 2, top, bw, bh).build());

        addRenderableWidget(Button.builder(Component.literal("❚❚  Стоять"), b -> runCmd("tars stay"))
                .bounds(cx - bw / 2, top + gap, bw, bh).build());

        addRenderableWidget(Button.builder(Component.literal("⚡  Бежать (перекат)"), b -> runCmd("tars sprint"))
                .bounds(cx - bw / 2, top + gap * 2, bw, bh).build());

        addRenderableWidget(Button.builder(Component.literal("Юмор 25%"), b -> runCmd("tars humor 25"))
                .bounds(cx - bw / 2, top + gap * 3, bw / 2 - 4, bh).build());

        addRenderableWidget(Button.builder(Component.literal("Юмор 75%"), b -> runCmd("tars humor 75"))
                .bounds(cx + 4, top + gap * 3, bw / 2 - 4, bh).build());

        addRenderableWidget(Button.builder(Component.literal("Юмор 100%"), b -> runCmd("tars humor 100"))
                .bounds(cx - bw / 2, top + gap * 4, bw, bh).build());

        addRenderableWidget(Button.builder(Component.literal("Статус"), b -> runCmd("tars status"))
                .bounds(cx - bw / 2, top + gap * 5, bw / 2 - 4, bh).build());

        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(cx + 4, top + gap * 5, bw / 2 - 4, bh).build());
    }

    private void runCmd(String cmd) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.connection.sendCommand(cmd);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g, mouseX, mouseY, partial);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int x0 = cx - PANEL_W / 2;
        int y0 = cy - PANEL_H / 2;

        g.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, 0xE0101018);
        g.fill(x0, y0, x0 + PANEL_W, y0 + 22, 0xFF2A2A35);
        g.renderOutline(x0, y0, PANEL_W, PANEL_H, 0xFFC8A030);

        g.drawCenteredString(this.font, "§6TARS · панель", cx, y0 + 7, 0xFFFFFF);

        String sub = "Юмор: " + tars.getHumor() + "%  ·  "
                + (tars.isFollowing() ? "следует" : "стоит")
                + (tars.isSprintMode() ? "  ·  бег" : "");
        g.drawCenteredString(this.font, sub, cx, y0 + PANEL_H - 14, 0xAAAAAA);

        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
