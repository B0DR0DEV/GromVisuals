package dev.gromclient.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class VisualsScreen extends Screen {
    private int panelX, panelY, panelW = 250, panelH = 236;
    private int mouseX, mouseY;

    public VisualsScreen() {
        super(Text.literal("GromVisuals"));
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFFB66BFF);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF20D0B13);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 35, 0xFF21152E);
        ctx.drawText(textRenderer, "GROM", panelX + 14, panelY + 9, 0xFFB66BFF, false);
        ctx.drawText(textRenderer, "VISUALS", panelX + 54, panelY + 9, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, "RIGHT SHIFT  /  VISUAL SETTINGS", panelX + 14, panelY + 23, 0xFFB9A9C9, false);

        VisualConfig c = GromVisualsClient.CONFIG;
        row(ctx, 0, "FPS Counter", c.fps);
        row(ctx, 1, "Coordinates XYZ", c.coordinates);
        row(ctx, 2, "CPS Counter", c.cps);
        row(ctx, 3, "Keystrokes", c.keystrokes);
        row(ctx, 4, "Direction", c.direction);

        ctx.fill(panelX + 12, panelY + 193, panelX + panelW - 12, panelY + 194, 0x664D3866);
        ctx.drawText(textRenderer, "Purple Neon  •  Client-side", panelX + 14, panelY + 204, 0xFFB9A9C9, false);
        ctx.fill(panelX + panelW - 75, panelY + 213, panelX + panelW - 12, panelY + 231, 0xFF492A70);
        ctx.drawText(textRenderer, "DONE", panelX + panelW - 55, panelY + 218, 0xFFFFFFFF, false);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void row(DrawContext ctx, int index, String label, boolean enabled) {
        int y = panelY + 45 + index * 29;
        boolean hovered = mouseX >= panelX + 12 && mouseX <= panelX + panelW - 12
                && mouseY >= y && mouseY <= y + 23;
        ctx.fill(panelX + 10, y, panelX + panelW - 10, y + 24,
                hovered ? 0xFF2A1B3B : 0xCC17131F);
        ctx.drawText(textRenderer, label, panelX + 18, y + 8, 0xFFF4EDFF, false);
        int color = enabled ? 0xFF8E4DCE : 0xFF514858;
        ctx.fill(panelX + panelW - 42, y + 6, panelX + panelW - 18, y + 18, color);
        ctx.fill(panelX + panelW - (enabled ? 24 : 40), y + 7,
                panelX + panelW - (enabled ? 19 : 35), y + 17, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0) {
            VisualConfig c = GromVisualsClient.CONFIG;
            for (int i = 0; i < 5; i++) {
                int y = panelY + 45 + i * 29;
                if (mouseX >= panelX + 10 && mouseX <= panelX + panelW - 10
                        && mouseY >= y && mouseY <= y + 24) {
                    switch (i) {
                        case 0 -> c.fps = !c.fps;
                        case 1 -> c.coordinates = !c.coordinates;
                        case 2 -> c.cps = !c.cps;
                        case 3 -> c.keystrokes = !c.keystrokes;
                        case 4 -> c.direction = !c.direction;
                    }
                    c.save();
                    return true;
                }
            }
            if (mouseX >= panelX + panelW - 75 && mouseX <= panelX + panelW - 12
                    && mouseY >= panelY + 213 && mouseY <= panelY + 231) {
                close();
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
