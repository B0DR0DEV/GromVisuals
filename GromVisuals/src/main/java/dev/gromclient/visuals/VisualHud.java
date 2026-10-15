package dev.gromclient.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Direction;

public final class VisualHud {
    private static final int PANEL = 0xB9121020;
    private static final int WHITE = 0xFFF4EDFF;
    private static final int MUTED = 0xFFB9A9C9;

    private VisualHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        VisualConfig c = GromVisualsClient.CONFIG;
        int x = 8, y = 8, width = 132;
        ctx.fill(x, y, x + width, y + 18, PANEL);
        ctx.fill(x, y, x + 2, y + 18, c.accent);
        ctx.drawText(client.textRenderer, "GROM", x + 8, y + 5, c.accent, false);
        ctx.drawText(client.textRenderer, "VISUALS", x + 49, y + 5, WHITE, false);
        y += 22;

        if (c.fps) {
            panel(ctx, x, y, width, 16, c.accent);
            ctx.drawText(client.textRenderer, "FPS", x + 7, y + 4, MUTED, false);
            ctx.drawText(client.textRenderer, Integer.toString(client.getCurrentFps()), x + width - 29, y + 4, WHITE, false);
            y += 18;
        }
        if (c.coordinates && client.player != null) {
            panel(ctx, x, y, width, 28, c.accent);
            ctx.drawText(client.textRenderer, "XYZ", x + 7, y + 4, c.accent, false);
            String coords = String.format("%.1f %.1f %.1f",
                    client.player.getX(), client.player.getY(), client.player.getZ());
            ctx.drawText(client.textRenderer, coords, x + 7, y + 16, WHITE, false);
            y += 30;
        }
        if (c.direction && client.player != null) {
            Direction d = client.player.getHorizontalFacing();
            panel(ctx, x, y, width, 16, c.accent);
            ctx.drawText(client.textRenderer, "FACING", x + 7, y + 4, MUTED, false);
            ctx.drawText(client.textRenderer, d.getName().toUpperCase(), x + 74, y + 4, WHITE, false);
        }

        if (c.cps) {
            String cps = "LMB " + c.leftCps() + "  RMB " + c.rightCps();
            int w = client.textRenderer.getWidth(cps) + 16;
            int bx = 8, by = client.getWindow().getScaledHeight() - 28;
            panel(ctx, bx, by, w, 20, c.accent);
            ctx.drawText(client.textRenderer, cps, bx + 8, by + 6, WHITE, false);
        }

        if (c.keystrokes) renderKeys(ctx, client, c);
    }

    private static void panel(DrawContext ctx, int x, int y, int w, int h, int accent) {
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + 2, y + h, accent);
        ctx.fill(x + 2, y + h - 1, x + w, y + h, 0x553D2455);
    }

    private static void renderKeys(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        int size = 20, gap = 3;
        int x = client.getWindow().getScaledWidth() - 8 - (size * 3 + gap * 2);
        int y = client.getWindow().getScaledHeight() - 74;
        key(ctx, client, x + size + gap, y, size, "W", client.options.forwardKey.isPressed(), c.accent);
        key(ctx, client, x, y + size + gap, size, "A", client.options.leftKey.isPressed(), c.accent);
        key(ctx, client, x + size + gap, y + size + gap, size, "S", client.options.backKey.isPressed(), c.accent);
        key(ctx, client, x + (size + gap) * 2, y + size + gap, size, "D", client.options.rightKey.isPressed(), c.accent);
        key(ctx, client, x, y + (size + gap) * 2, size * 3 + gap * 2, "SPACE",
                client.options.jumpKey.isPressed(), c.accent);
    }

    private static void key(DrawContext ctx, MinecraftClient client, int x, int y, int size,
                            String label, boolean pressed, int accent) {
        int bg = pressed ? 0xDD492A70 : PANEL;
        ctx.fill(x, y, x + size, y + size, bg);
        ctx.fill(x, y, x + size, y + 1, accent);
        int tx = x + (size - client.textRenderer.getWidth(label)) / 2;
        ctx.drawText(client.textRenderer, label, tx, y + (size - 8) / 2, WHITE, false);
    }
}
