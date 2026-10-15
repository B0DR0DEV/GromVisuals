package dev.gromclient.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Custom settings screen. Deliberately draws its own background: Screen.renderBackground
 * uses the vanilla blur pipeline, which can throw "Can only blur once per frame" on 1.21.11. */
public final class VisualsScreen extends Screen {
    private static final int PURPLE = 0xFFB66BFF;
    private static final int WHITE = 0xFFF5F0FF;
    private static final int MUTED = 0xFFB9A9C9;
    private static final int PANEL = 0xF20D0B13;
    private static final int ROW = 0xEE191420;
    private int panelX, panelY, panelW = 510, panelH = 390;
    private int mouseX, mouseY, scroll;
    private int page = 0;
    private final List<Setting> visibleSettings = new ArrayList<>();

    private record Setting(String label, String description, Getter getter, Setter setter) {}
    @FunctionalInterface private interface Getter { boolean get(VisualConfig c); }
    @FunctionalInterface private interface Setter { void set(VisualConfig c, boolean value); }

    public VisualsScreen() { super(Text.literal("GromVisuals")); }

    @Override protected void init() {
        panelW = Math.min(590, width - 24);
        panelH = Math.min(430, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        scroll = 0;
    }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX; this.mouseY = mouseY;
        // No renderBackground(): it invokes the blur pipeline and caused the reported crash.
        ctx.fill(0, 0, width, height, 0xE9080710);
        for (int i = 0; i < height; i += 4) {
            int alpha = Math.max(0, 12 - (i / Math.max(1, height / 12)));
            if (alpha > 0) ctx.fill(0, i, width, i + 2, (alpha << 24) | 0x003D1761);
        }
        ctx.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PURPLE);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 58, 0xFF171020);
        ctx.fill(panelX, panelY, panelX + 4, panelY + panelH, PURPLE);
        ctx.drawText(textRenderer, "GROM", panelX + 18, panelY + 13, PURPLE, false);
        ctx.drawText(textRenderer, "VISUALS", panelX + 68, panelY + 13, WHITE, false);
        ctx.drawText(textRenderer, "CLIENT CUSTOMIZATION  /  1.21.11", panelX + 18, panelY + 31, MUTED, false);
        ctx.drawText(textRenderer, "RIGHT SHIFT", panelX + panelW - 88, panelY + 15, MUTED, false);

        String[] tabs = {"HUD", "VISUALS", "COSMETICS", "GENERAL"};
        int tabY = panelY + 65, tabW = (panelW - 34) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int x = panelX + 12 + i * tabW;
            boolean selected = page == i;
            ctx.fill(x, tabY, x + tabW - 4, tabY + 23, selected ? 0xFF492A70 : 0xFF201927);
            if (selected) ctx.fill(x, tabY + 21, x + tabW - 4, tabY + 23, PURPLE);
            centered(ctx, tabs[i], x, tabY + 8, tabW - 4, selected ? WHITE : MUTED);
        }

        visibleSettings.clear();
        populateSettings();
        int contentTop = panelY + 99;
        int contentBottom = panelY + panelH - 39;
        ctx.enableScissor(panelX + 10, contentTop, panelX + panelW - 10, contentBottom);
        for (int i = 0; i < visibleSettings.size(); i++) {
            Setting s = visibleSettings.get(i);
            int y = contentTop + i * 43 - scroll;
            if (y + 38 < contentTop || y > contentBottom) continue;
            boolean hovered = mouseX >= panelX + 12 && mouseX <= panelX + panelW - 12
                    && mouseY >= y && mouseY <= y + 38;
            ctx.fill(panelX + 12, y, panelX + panelW - 12, y + 38, hovered ? 0xFF2A2035 : ROW);
            ctx.drawText(textRenderer, s.label(), panelX + 22, y + 7, WHITE, false);
            ctx.drawText(textRenderer, s.description(), panelX + 22, y + 22, MUTED, false);
            boolean enabled = s.getter().get(GromVisualsClient.CONFIG);
            int sx = panelX + panelW - 52;
            ctx.fill(sx, y + 11, sx + 27, y + 26, enabled ? 0xFF7544A6 : 0xFF49434F);
            int knobX = enabled ? sx + 16 : sx + 3;
            ctx.fill(knobX, y + 13, knobX + 9, y + 24, 0xFFFFFFFF);
        }
        ctx.disableScissor();

        int footerY = panelY + panelH - 29;
        ctx.fill(panelX + 12, footerY - 5, panelX + panelW - 12, footerY - 4, 0x664D3866);
        ctx.drawText(textRenderer, "THEME: " + GromVisualsClient.CONFIG.theme.toUpperCase(), panelX + 16, footerY + 4, MUTED, false);
        int themeX = panelX + panelW - 174;
        ctx.fill(themeX, footerY, themeX + 76, footerY + 19, 0xFF30213D);
        centered(ctx, "THEME", themeX, footerY + 6, 76, WHITE);
        ctx.fill(panelX + panelW - 90, footerY, panelX + panelW - 12, footerY + 19, 0xFF492A70);
        centered(ctx, "DONE", panelX + panelW - 90, footerY + 6, 78, WHITE);
    }

    private void populateSettings() {
        VisualConfig c = GromVisualsClient.CONFIG;
        switch (page) {
            case 0 -> {
                add("FPS Counter", "Current frames per second", x -> x.fps, (x,v)->x.fps=v);
                add("Coordinates XYZ", "Player X / Y / Z position", x -> x.coordinates, (x,v)->x.coordinates=v);
                add("CPS Counter", "Left and right mouse clicks per second", x -> x.cps, (x,v)->x.cps=v);
                add("Keystrokes", "W A S D and jump key overlay", x -> x.keystrokes, (x,v)->x.keystrokes=v);
                add("Mouse buttons", "Show LMB / RMB in keystrokes", x -> x.keystrokeMouseButtons, (x,v)->x.keystrokeMouseButtons=v);
                add("Direction", "Current horizontal facing direction", x -> x.direction, (x,v)->x.direction=v);
                add("Ping", "Server latency in milliseconds", x -> x.ping, (x,v)->x.ping=v);
                add("Clock", "In-game time display", x -> x.clock, (x,v)->x.clock=v);
                add("Movement speed", "Approximate horizontal speed", x -> x.speed, (x,v)->x.speed=v);
                add("Biome", "Current biome identifier", x -> x.biome, (x,v)->x.biome=v);
                add("Armor status", "Armor items and durability bars", x -> x.armor, (x,v)->x.armor=v);
                add("Held item durability", "Durability of the item in hand", x -> x.durability, (x,v)->x.durability=v);
                add("Potion effects", "Active effect names and timers", x -> x.potionEffects, (x,v)->x.potionEffects=v);
                add("Custom crosshair", "Draw a configurable client-side crosshair", x -> x.crosshair, (x,v)->x.crosshair=v);
                add("Target HUD", "Health and distance for the entity under your crosshair", x -> x.targetHud, (x,v)->x.targetHud=v);
                add("Saturation HUD", "Food and saturation levels", x -> x.saturationHud, (x,v)->x.saturationHud=v);
                add("Low health warning", "Show a warning when your health is low", x -> x.lowHealthWarning, (x,v)->x.lowHealthWarning=v);
                add("Armor notifier", "Warn when armor durability is low", x -> x.armorNotifier, (x,v)->x.armorNotifier=v);
            }
            case 1 -> {
                add("HUD title", "Show GROM VISUALS header", x -> x.hudTitle, (x,v)->x.hudTitle=v);
                add("Compact HUD", "Use tighter module spacing", x -> x.compactHud, (x,v)->x.compactHud=v);
                add("HUD animations", "Enable animated visual states", x -> x.animations, (x,v)->x.animations=v);
                add("Show over menus", "Keep HUD visible while screens are open", x -> x.showInScreens, (x,v)->x.showInScreens=v);
            }
            case 2 -> {
                add("China Hat", "Cosmetic option — renderer integration planned", x -> x.cosmeticChinaHat, (x,v)->x.cosmeticChinaHat=v);
                add("Halo", "Cosmetic option — renderer integration planned", x -> x.cosmeticHalo, (x,v)->x.cosmeticHalo=v);
                add("Angel Wings", "Cosmetic option — renderer integration planned", x -> x.cosmeticWings, (x,v)->x.cosmeticWings=v);
                add("Devil Horns", "Cosmetic option — renderer integration planned", x -> x.cosmeticDevilHorns, (x,v)->x.cosmeticDevilHorns=v);
                add("Cosmetic Particles", "Cosmetic option — renderer integration planned", x -> x.cosmeticParticles, (x,v)->x.cosmeticParticles=v);
            }
            default -> {
                add("Theme", "Use the button at the bottom to cycle themes", x -> true, (x,v)->{});
                add("Purple accent", "Purple neon theme", x -> "Purple".equals(x.theme), (x,v)->{if(v){x.theme="Purple";x.accent=0xFFB66BFF;}});
                add("Neon accent", "Cyan neon theme", x -> "Neon".equals(x.theme), (x,v)->{if(v){x.theme="Neon";x.accent=0xFF35E6FF;}});
                add("GROM accent", "Orange accent theme", x -> "GROM".equals(x.theme), (x,v)->{if(v){x.theme="GROM";x.accent=0xFFFF7A35;}});
            }
        }
    }

    private void add(String label, String description, Getter getter, Setter setter) {
        visibleSettings.add(new Setting(label, description, getter, setter));
    }

    private void centered(DrawContext ctx, String text, int x, int y, int w, int color) {
        ctx.drawText(textRenderer, text, x + (w - textRenderer.getWidth(text)) / 2, y, color, false);
    }

    @Override public boolean mouseClicked(Click click, boolean doubleClick) {
        double mx = click.x(), my = click.y();
        if (click.button() != 0) return super.mouseClicked(click, doubleClick);
        int tabY = panelY + 65, tabW = (panelW - 34) / 4;
        if (my >= tabY && my <= tabY + 23 && mx >= panelX + 12 && mx <= panelX + panelW - 12) {
            page = Math.max(0, Math.min(3, (int)((mx - panelX - 12) / tabW)));
            scroll = 0;
            return true;
        }
        int footerY = panelY + panelH - 29;
        if (my >= footerY && my <= footerY + 19) {
            if (mx >= panelX + panelW - 90 && mx <= panelX + panelW - 12) { close(); return true; }
            if (mx >= panelX + panelW - 174 && mx <= panelX + panelW - 98) {
                GromVisualsClient.CONFIG.cycleTheme(); return true;
            }
        }
        int contentTop = panelY + 99, contentBottom = panelY + panelH - 39;
        if (my >= contentTop && my <= contentBottom) {
            int index = (int)((my - contentTop + scroll) / 43);
            if (index >= 0 && index < visibleSettings.size()) {
                Setting s = visibleSettings.get(index);
                VisualConfig c = GromVisualsClient.CONFIG;
                s.setter().set(c, !s.getter().get(c));
                c.save();
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, visibleSettings.size() * 43 - (panelH - 145));
        scroll = Math.max(0, Math.min(max, scroll - (int)(verticalAmount * 43)));
        return true;
    }

    @Override public boolean shouldPause() { return false; }
}
