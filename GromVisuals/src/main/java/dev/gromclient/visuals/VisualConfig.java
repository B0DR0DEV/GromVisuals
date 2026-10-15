package dev.gromclient.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/** Persistent client-side settings for GromVisuals. */
public final class VisualConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config").resolve("grom-visuals.json");

    // HUD modules
    public boolean fps = true;
    public boolean coordinates = true;
    public boolean cps = true;
    public boolean keystrokes = true;
    public boolean direction = true;
    public boolean ping = true;
    public boolean clock = false;
    public boolean speed = false;
    public boolean biome = false;
    public boolean armor = true;
    public boolean durability = false;
    public boolean potionEffects = true;
    public boolean crosshair = false;
    public boolean keystrokeMouseButtons = true;
    public boolean hudTitle = true;
    public boolean showInScreens = false;
    public boolean saturationHud = false;
    public boolean armorNotifier = true;
    public boolean targetHud = true;
    public boolean lowHealthWarning = false;

    // Appearance
    public int accent = 0xFFB66BFF;
    public String theme = "Purple";
    public float hudScale = 1.0f;
    public int panelAlpha = 185;
    public boolean compactHud = false;
    public boolean animations = true;
    public boolean cosmeticChinaHat = false;
    public boolean cosmeticHalo = false;
    public boolean cosmeticWings = false;
    public boolean cosmeticDevilHorns = false;
    public boolean cosmeticParticles = false;

    private transient Deque<Long> leftClicks = new ArrayDeque<>();
    private transient Deque<Long> rightClicks = new ArrayDeque<>();
    private transient long leftClickUntil;
    private transient long rightClickUntil;

    public void tick(MinecraftClient client) {
        if (leftClicks == null) leftClicks = new ArrayDeque<>();
        if (rightClicks == null) rightClicks = new ArrayDeque<>();
        if (client.player == null || client.currentScreen != null) return;
        long now = System.currentTimeMillis();
        trimClicks(leftClicks, now);
        trimClicks(rightClicks, now);
        if (client.options.attackKey.wasPressed()) {
            leftClicks.addLast(now);
            leftClickUntil = now + 140;
        }
        if (client.options.useKey.wasPressed()) {
            rightClicks.addLast(now);
            rightClickUntil = now + 140;
        }
    }

    private static void trimClicks(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
    }

    public int leftCps() { if (leftClicks == null) leftClicks = new ArrayDeque<>(); return leftClicks.size(); }
    public int rightCps() { if (rightClicks == null) rightClicks = new ArrayDeque<>(); return rightClicks.size(); }
    public boolean leftPressed() { return System.currentTimeMillis() < leftClickUntil; }
    public boolean rightPressed() { return System.currentTimeMillis() < rightClickUntil; }

    public void cycleTheme() {
        switch (theme) {
            case "GROM" -> { theme = "Purple"; accent = 0xFFB66BFF; }
            case "Purple" -> { theme = "Neon"; accent = 0xFF35E6FF; }
            case "Neon" -> { theme = "White"; accent = 0xFFFFFFFF; }
            default -> { theme = "GROM"; accent = 0xFFFF7A35; }
        }
        save();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[GromVisuals] Не удалось сохранить конфиг: " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (Files.exists(FILE)) {
                VisualConfig loaded = GSON.fromJson(Files.readString(FILE), VisualConfig.class);
                if (loaded != null) copySettings(loaded);
            }
        } catch (Exception e) {
            System.err.println("[GromVisuals] Конфиг не прочитан, используются значения по умолчанию: " + e.getMessage());
        }
    }

    private void copySettings(VisualConfig c) {
        fps=c.fps; coordinates=c.coordinates; cps=c.cps; keystrokes=c.keystrokes; direction=c.direction;
        ping=c.ping; clock=c.clock; speed=c.speed; biome=c.biome; armor=c.armor; durability=c.durability;
        potionEffects=c.potionEffects; crosshair=c.crosshair; keystrokeMouseButtons=c.keystrokeMouseButtons;
        hudTitle=c.hudTitle; showInScreens=c.showInScreens; saturationHud=c.saturationHud;
        armorNotifier=c.armorNotifier; targetHud=c.targetHud; lowHealthWarning=c.lowHealthWarning; accent=c.accent;
        theme=c.theme == null ? "Purple" : c.theme; hudScale=clamp(c.hudScale, 0.6f, 1.6f);
        panelAlpha=Math.max(70, Math.min(255, c.panelAlpha)); compactHud=c.compactHud;
        animations=c.animations; cosmeticChinaHat=c.cosmeticChinaHat; cosmeticHalo=c.cosmeticHalo;
        cosmeticWings=c.cosmeticWings; cosmeticDevilHorns=c.cosmeticDevilHorns; cosmeticParticles=c.cosmeticParticles;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
