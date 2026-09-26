package com.simplespace.client;

import com.simplespace.tars.TarsEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TarsMenuScreen extends Screen {

    private final TarsEntity tars;
    private static final int PANEL_W = 230;
    private static final int PANEL_H = 190;

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
        int top = this.height / 2 - PANEL_H / 2 + 30;
        int bw = 190;
        int bh = 20;
        int gap = 24;

        addRenderableWidget(Button.builder(Component.literal("Следовать"), b -> runCmd("tars follow"))
                .bounds(cx - bw / 2, top, bw, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Стоять"), b -> runCmd("tars stay"))
                .bounds(cx - bw / 2, top + gap, bw, bh).build());
        addRenderableWidget(Button.builder(Component.literal("Бежать (перекат)"), b -> runCmd("tars sprint"))
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
        g.fill(0, 0, this.width, this.height, 0xCC000000);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int x0 = cx - PANEL_W / 2;
        int y0 = cy - PANEL_H / 2;

        g.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, 0xFF14141C);
        g.fill(x0, y0, x0 + PANEL_W, y0 + 24, 0xFF1E1E2A);
        g.renderOutline(x0, y0, PANEL_W, PANEL_H, 0xFF3A3A48);

        g.drawCenteredString(this.font, "TARS", cx, y0 + 8, 0xE0C040);

        String sub = "Юмор " + tars.getHumor() + "% · "
                + (tars.isFollowing() ? "следует" : "стоит")
                + (tars.isSprintMode() ? " · бег" : "");
        g.drawCenteredString(this.font, sub, cx, y0 + PANEL_H - 14, 0x888888);

        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
