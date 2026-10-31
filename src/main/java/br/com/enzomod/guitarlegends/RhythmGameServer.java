package br.com.enzomod.guitarlegends;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Compatibilidade com os pacotes antigos da versao 3D.
 * O jogo de ritmo final funciona inteiramente pela GuitarScreen no cliente.
 * Estes metodos permanecem apenas para que servidores/mundos antigos nao quebrem.
 */
public final class RhythmGameServer {
    private RhythmGameServer() {}

    public static void start(ServerPlayerEntity player, int song) {
        // Modo antigo de trilhos/carrinhos desativado de proposito.
    }

    public static void onLaneInput(ServerPlayerEntity player, int lane, boolean pressed) {
        // O input agora e tratado diretamente na GUI.
    }

    public static void tick(MinecraftServer server) {
        // Nenhuma arena fisica e criada no mundo.
    }
}
