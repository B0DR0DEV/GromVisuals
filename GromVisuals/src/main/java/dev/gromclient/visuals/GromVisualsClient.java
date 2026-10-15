package dev.gromclient.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class GromVisualsClient implements ClientModInitializer {
    public static final VisualConfig CONFIG = new VisualConfig();
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        CONFIG.load();
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.grom_visuals.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen instanceof VisualsScreen) {
                    client.setScreen(null);
                } else if (client.currentScreen == null) {
                    client.setScreen(new VisualsScreen());
                }
            }
            CONFIG.tick(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null && (client.currentScreen == null || CONFIG.showInScreens)) {
                VisualHud.render(drawContext, client);
            }
        });
    }
}
