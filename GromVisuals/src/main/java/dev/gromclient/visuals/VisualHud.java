package dev.gromclient.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Client-only visual modules. Kept on vanilla DrawContext APIs for compatibility. */
public final class VisualHud {
    private static final int WHITE = 0xFFF4EDFF;
    private static final int MUTED = 0xFFB9A9C9;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private VisualHud() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        VisualConfig c = GromVisualsClient.CONFIG;
        if (client.player == null || client.world == null) return;
        int scale = MathHelper.clamp(Math.round(c.hudScale * 100), 60, 160);
        // Scale is currently applied to spacing/width, avoiding a matrix transform that can conflict with other HUD mods.
        int x = 8, y = 8, width = Math.round((c.compactHud ? 116 : 148) * scale / 100.0f);
        int rowH = c.compactHud ? 14 : 17;
        if (c.hudTitle) {
            panel(ctx, x, y, width, rowH + 2, c, true);
            ctx.drawText(client.textRenderer, "GROM", x + 7, y + 4, c.accent, false);
            ctx.drawText(client.textRenderer, "VISUALS", x + 47, y + 4, WHITE, false);
            y += rowH + 4;
        }

        if (c.fps) { row(ctx, client, c, x, y, width, "FPS", Integer.toString(client.getCurrentFps()), rowH); y += rowH + 2; }
        if (c.coordinates) {
            String coords = String.format(java.util.Locale.ROOT, "%.1f  %.1f  %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
            row(ctx, client, c, x, y, width, "XYZ", coords, rowH); y += rowH + 2;
        }
        if (c.direction) { row(ctx, client, c, x, y, width, "FACING", direction(client.player.getHorizontalFacing()), rowH); y += rowH + 2; }
        if (c.ping) {
            int latency = -1;
            if (client.getNetworkHandler() != null) {
                var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) latency = entry.getLatency();
            }
            row(ctx, client, c, x, y, width, "PING", latency < 0 ? "--" : latency + " ms", rowH); y += rowH + 2;
        }
        if (c.clock) { row(ctx, client, c, x, y, width, "TIME", CLOCK.format(LocalTime.now()), rowH); y += rowH + 2; }
        if (c.speed) {
            double speed = client.player.getVelocity().horizontalLength() * 20.0;
            row(ctx, client, c, x, y, width, "SPEED", String.format(java.util.Locale.ROOT, "%.2f m/s", speed), rowH); y += rowH + 2;
        }
        if (c.biome) {
            String biome = client.world.getBiome(client.player.getBlockPos()).getKey()
                    .map(key -> key.getValue().getPath()).orElse("unknown");
            row(ctx, client, c, x, y, width, "BIOME", trim(biome, 18), rowH); y += rowH + 2;
        }

        if (c.cps) {
            String value = "LMB " + c.leftCps() + "   RMB " + c.rightCps();
            int bx = 8, by = client.getWindow().getScaledHeight() - (c.keystrokes ? 103 : 29);
            panel(ctx, bx, by, client.textRenderer.getWidth(value) + 18, 21, c, false);
            ctx.drawText(client.textRenderer, value, bx + 8, by + 6, WHITE, false);
        }
        if (c.keystrokes) renderKeys(ctx, client, c);
        if (c.armor) renderArmor(ctx, client, c);
        if (c.durability) renderHeldDurability(ctx, client, c);
        if (c.potionEffects) renderEffects(ctx, client, c);
        if (c.crosshair) renderCrosshair(ctx, client, c);
        if (c.targetHud && client.targetedEntity instanceof LivingEntity target && target.isAlive()) renderTargetHud(ctx, client, c, target);
        if (c.saturationHud) renderSaturation(ctx, client, c);
        if (c.armorNotifier) renderArmorWarning(ctx, client, c);
        if (c.lowHealthWarning && client.player.getHealth() <= 6.0f) renderLowHealth(ctx, client, c);
    }

    private static String direction(Direction d) {
        return switch (d) { case NORTH -> "NORTH"; case SOUTH -> "SOUTH"; case EAST -> "EAST"; case WEST -> "WEST"; default -> d.name(); };
    }

    private static String trim(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }

    private static void panel(DrawContext ctx, int x, int y, int w, int h, VisualConfig c, boolean header) {
        int alpha = MathHelper.clamp(c.panelAlpha, 70, 255);
        int bg = (alpha << 24) | 0x00100C18;
        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + 2, y + h, c.accent);
        if (!c.compactHud) ctx.fill(x + 2, y + h - 1, x + w, y + h, 0x553D2455);
    }

    private static void row(DrawContext ctx, MinecraftClient client, VisualConfig c, int x, int y, int w,
                            String label, String value, int h) {
        panel(ctx, x, y, w, h, c, false);
        ctx.drawText(client.textRenderer, label, x + 7, y + 4, c.accent, false);
        int valueX = x + w - 7 - client.textRenderer.getWidth(value);
        if (valueX < x + 38) valueX = x + 38;
        ctx.drawText(client.textRenderer, value, valueX, y + 4, WHITE, false);
    }

    private static void renderKeys(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        int size = c.compactHud ? 17 : 20, gap = 3;
        int extraRows = c.keystrokeMouseButtons ? 1 : 0;
        int blockWidth = size * 3 + gap * 2;
        int x = client.getWindow().getScaledWidth() - 8 - blockWidth;
        int y = client.getWindow().getScaledHeight() - 8 - (size * (3 + extraRows) + gap * (2 + extraRows));
        key(ctx, client, x + size + gap, y, size, "W", client.options.forwardKey.isPressed(), c);
        key(ctx, client, x, y + size + gap, size, "A", client.options.leftKey.isPressed(), c);
        key(ctx, client, x + size + gap, y + size + gap, size, "S", client.options.backKey.isPressed(), c);
        key(ctx, client, x + (size + gap) * 2, y + size + gap, size, "D", client.options.rightKey.isPressed(), c);
        key(ctx, client, x, y + (size + gap) * 2, blockWidth, "SPACE", client.options.jumpKey.isPressed(), c);
        if (c.keystrokeMouseButtons) {
            int my = y + (size + gap) * 3;
            key(ctx, client, x, my, (blockWidth - gap) / 2, "LMB", c.leftPressed(), c);
            key(ctx, client, x + (blockWidth + gap) / 2, my, (blockWidth - gap) / 2, "RMB", c.rightPressed(), c);
        }
    }

    private static void key(DrawContext ctx, MinecraftClient client, int x, int y, int w, String label,
                            boolean pressed, VisualConfig c) {
        int h = c.compactHud ? 17 : 20;
        ctx.fill(x, y, x + w, y + h, pressed ? 0xEE492A70 : 0xB9121020);
        ctx.fill(x, y, x + w, y + 1, c.accent);
        int tx = x + (w - client.textRenderer.getWidth(label)) / 2;
        ctx.drawText(client.textRenderer, label, tx, y + (h - 8) / 2, WHITE, false);
    }

    private static void renderArmor(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        int x = 8, y = client.getWindow().getScaledHeight() / 2 - 42;
        int index = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (stack.isEmpty()) continue;
            int iy = y + index * 20;
            ctx.drawItem(stack, x, iy);
            if (stack.isDamageable()) {
                int remaining = stack.getMaxDamage() - stack.getDamage();
                float ratio = remaining / (float) stack.getMaxDamage();
                int color = ratio > .5f ? 0xFF67E58B : ratio > .2f ? 0xFFFFC857 : 0xFFFF5555;
                ctx.fill(x + 18, iy + 13, x + 34, iy + 16, 0xFF302A35);
                ctx.fill(x + 18, iy + 13, x + 18 + Math.round(16 * ratio), iy + 16, color);
            }
            index++;
        }
    }

    private static void renderHeldDurability(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty() || !stack.isDamageable()) return;
        int remaining = stack.getMaxDamage() - stack.getDamage();
        String text = "ITEM " + remaining + "/" + stack.getMaxDamage();
        int x = 8, y = client.getWindow().getScaledHeight() - 28;
        panel(ctx, x, y, client.textRenderer.getWidth(text) + 18, 20, c, false);
        ctx.drawText(client.textRenderer, text, x + 8, y + 6, WHITE, false);
    }

    private static void renderEffects(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        int x = client.getWindow().getScaledWidth() - 150, y = 8;
        for (var effect : client.player.getStatusEffects()) {
            String name = effect.getEffectType().value().getName().getString();
            String line = trim(name, 16) + " " + formatDuration(effect.getDuration());
            panel(ctx, x, y, 142, 17, c, false);
            ctx.drawText(client.textRenderer, line, x + 7, y + 4, WHITE, false);
            y += 19;
            if (y > client.getWindow().getScaledHeight() - 20) break;
        }
    }

    private static String formatDuration(int ticks) {
        if (ticks < 0) return "∞";
        int seconds = ticks / 20;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }


    private static void renderTargetHud(DrawContext ctx, MinecraftClient client, VisualConfig c, LivingEntity target) {
        String name = trim(target.getName().getString(), 20);
        String health = String.format(java.util.Locale.ROOT, "%.1f HP", target.getHealth());
        String distance = String.format(java.util.Locale.ROOT, "%.1f m", Math.sqrt(client.player.squaredDistanceTo(target)));
        int x = client.getWindow().getScaledWidth() / 2 + 14;
        int y = client.getWindow().getScaledHeight() / 2 + 12;
        int w = Math.max(client.textRenderer.getWidth(name), client.textRenderer.getWidth(health + "  " + distance)) + 16;
        panel(ctx, x, y, w, 35, c, false);
        ctx.drawText(client.textRenderer, name, x + 8, y + 5, c.accent, false);
        ctx.drawText(client.textRenderer, health + "  " + distance, x + 8, y + 19, WHITE, false);
        int barW = Math.max(1, w - 16);
        ctx.fill(x + 8, y + 30, x + 8 + barW, y + 33, 0xFF382D40);
        float ratio = MathHelper.clamp(target.getHealth() / Math.max(1.0f, target.getMaxHealth()), 0.0f, 1.0f);
        ctx.fill(x + 8, y + 30, x + 8 + Math.round(barW * ratio), y + 33, ratio > .5f ? 0xFF71E58B : 0xFFFF5E7D);
    }

    private static void renderSaturation(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        int food = client.player.getHungerManager().getFoodLevel();
        float saturation = client.player.getHungerManager().getSaturationLevel();
        String text = "FOOD " + food + "/20  SAT " + String.format(java.util.Locale.ROOT, "%.1f", saturation);
        int x = 8, y = client.getWindow().getScaledHeight() - 51;
        panel(ctx, x, y, client.textRenderer.getWidth(text) + 18, 19, c, false);
        ctx.drawText(client.textRenderer, text, x + 8, y + 5, WHITE, false);
    }

    private static void renderArmorWarning(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.isDamageable()) {
                float ratio = (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
                if (ratio <= .15f) {
                    String text = "LOW ARMOR DURABILITY";
                    int x = (client.getWindow().getScaledWidth() - client.textRenderer.getWidth(text)) / 2;
                    panel(ctx, x - 8, 8, client.textRenderer.getWidth(text) + 16, 20, c, false);
                    ctx.drawText(client.textRenderer, text, x, 14, 0xFFFF6666, false);
                    return;
                }
            }
        }
    }

    private static void renderLowHealth(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        int w = client.getWindow().getScaledWidth(), h = client.getWindow().getScaledHeight();
        int red = 0x33FF2020;
        ctx.fill(0, 0, w, 3, red); ctx.fill(0, h - 3, w, h, red);
        ctx.fill(0, 0, 3, h, red); ctx.fill(w - 3, 0, w, h, red);
        String warning = "LOW HEALTH";
        int x = (w - client.textRenderer.getWidth(warning)) / 2;
        panel(ctx, x - 8, h / 2 + 24, client.textRenderer.getWidth(warning) + 16, 20, c, false);
        ctx.drawText(client.textRenderer, warning, x, h / 2 + 30, 0xFFFF6868, false);
    }

    private static void renderCrosshair(DrawContext ctx, MinecraftClient client, VisualConfig c) {
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        ctx.fill(cx - 7, cy, cx - 2, cy + 1, c.accent);
        ctx.fill(cx + 2, cy, cx + 7, cy + 1, c.accent);
        ctx.fill(cx, cy - 7, cx + 1, cy - 2, c.accent);
        ctx.fill(cx, cy + 2, cx + 1, cy + 7, c.accent);
    }
}
