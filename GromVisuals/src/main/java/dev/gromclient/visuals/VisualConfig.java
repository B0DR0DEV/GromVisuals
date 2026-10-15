package dev.gromclient.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VisualConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = MinecraftClient.getInstance().runDirectory.toPath()
            .resolve("config").resolve("grom-visuals.json");

    public boolean fps = true;
    public boolean coordinates = true;
    public boolean cps = true;
    public boolean keystrokes = true;
    public boolean direction = true;
    public int accent = 0xFFB66BFF;
    private long leftClickUntil;
    private long rightClickUntil;
    private int leftClicks;
    private int rightClicks;
    private boolean wasLeftDown;
    private boolean wasRightDown;
    private long cpsWindowStart = System.currentTimeMillis();

    public void tick(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) return;
        long now = System.currentTimeMillis();
        boolean left = client.options.attackKey.isPressed();
        boolean right = client.options.useKey.isPressed();
        if (left && !wasLeftDown) { leftClicks++; leftClickUntil = now + 150; }
        if (right && !wasRightDown) { rightClicks++; rightClickUntil = now + 150; }
        wasLeftDown = left;
        wasRightDown = right;
        if (now - cpsWindowStart >= 1000) {
            leftClicks = Math.min(leftClicks, 20);
            rightClicks = Math.min(rightClicks, 20);
            cpsWindowStart = now;
        }
    }

    public int leftCps() { return leftClicks; }
    public int rightCps() { return rightClicks; }
    public boolean leftPressed() { return System.currentTimeMillis() < leftClickUntil; }
    public boolean rightPressed() { return System.currentTimeMillis() < rightClickUntil; }

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
                if (loaded != null) {
                    fps = loaded.fps;
                    coordinates = loaded.coordinates;
                    cps = loaded.cps;
                    keystrokes = loaded.keystrokes;
                    direction = loaded.direction;
                    accent = loaded.accent;
                }
            }
        } catch (Exception e) {
            System.err.println("[GromVisuals] Конфиг не прочитан, используются значения по умолчанию.");
        }
    }
}
