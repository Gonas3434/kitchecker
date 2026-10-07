package de.kitchecker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KitCheckerClient implements ClientModInitializer {
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        KitStore.load();

        // Ab 1.21.9 braucht eine Taste eine Kategorie-Klasse (davor ein String).
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("kitchecker", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.kitchecker.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(new KitScreen());
                }
            }
        });
    }
}
