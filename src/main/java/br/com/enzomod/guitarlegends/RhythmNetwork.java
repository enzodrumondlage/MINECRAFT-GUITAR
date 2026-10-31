package br.com.enzomod.guitarlegends;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class RhythmNetwork {
    private RhythmNetwork() {}

    public static final Identifier START_3D = new Identifier("guitarlegends", "start_3d");
    public static final Identifier LANE_INPUT = new Identifier("guitarlegends", "lane_input");
    public static final Identifier PLAY_AUDIO = new Identifier("guitarlegends", "play_audio");
    public static final Identifier STOP_AUDIO = new Identifier("guitarlegends", "stop_audio");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(START_3D, (server, player, handler, buf, responseSender) -> {
            int song = buf.readVarInt();
            server.execute(() -> RhythmGameServer.start(player, song));
        });

        ServerPlayNetworking.registerGlobalReceiver(LANE_INPUT, (server, player, handler, buf, responseSender) -> {
            int lane = buf.readVarInt();
            boolean pressed = buf.readBoolean();
            server.execute(() -> RhythmGameServer.onLaneInput(player, lane, pressed));
        });

        ServerTickEvents.END_SERVER_TICK.register(RhythmGameServer::tick);
    }

    public static void playAudio(net.minecraft.server.network.ServerPlayerEntity player, int song, int delayMs) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(song);
        buf.writeVarInt(delayMs);
        ServerPlayNetworking.send(player, PLAY_AUDIO, buf);
    }

    public static void stopAudio(net.minecraft.server.network.ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, STOP_AUDIO, PacketByteBufs.empty());
    }
}
