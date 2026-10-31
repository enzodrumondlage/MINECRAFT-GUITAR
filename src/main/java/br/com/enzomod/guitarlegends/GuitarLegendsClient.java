package br.com.enzomod.guitarlegends;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.TypedActionResult;
import org.lwjgl.glfw.GLFW;

public class GuitarLegendsClient implements ClientModInitializer {
    private static final int[] KEYS = {
        GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K
    };
    private static final boolean[] PREVIOUS = new boolean[5];

    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getStackInHand(hand);
            if (!Registries.ITEM.getId(stack.getItem()).getNamespace().equals("guitarlegends")) {
                return TypedActionResult.pass(stack);
            }
            if (world.isClient) MinecraftClient.getInstance().setScreen(new GuitarScreen());
            return TypedActionResult.success(stack, world.isClient);
        });

        ClientPlayNetworking.registerGlobalReceiver(RhythmNetwork.PLAY_AUDIO, (client, handler, buf, responseSender) -> {
            int song = buf.readVarInt();
            int delayMs = buf.readVarInt();
            client.execute(() -> RhythmAudio.schedule(song, delayMs));
        });

        ClientPlayNetworking.registerGlobalReceiver(RhythmNetwork.STOP_AUDIO, (client, handler, buf, responseSender) ->
            client.execute(RhythmAudio::stop));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RhythmAudio.tick(client);
            if (!RhythmAudio.isGameActive() || client.player == null || client.getNetworkHandler() == null) {
                for (int i = 0; i < PREVIOUS.length; i++) PREVIOUS[i] = false;
                return;
            }

            long window = client.getWindow().getHandle();
            for (int lane = 0; lane < KEYS.length; lane++) {
                boolean pressed = GLFW.glfwGetKey(window, KEYS[lane]) == GLFW.GLFW_PRESS;
                if (pressed != PREVIOUS[lane]) {
                    PREVIOUS[lane] = pressed;
                    PacketByteBuf out = PacketByteBufs.create();
                    out.writeVarInt(lane);
                    out.writeBoolean(pressed);
                    ClientPlayNetworking.send(RhythmNetwork.LANE_INPUT, out);
                }
            }
        });
    }
}
