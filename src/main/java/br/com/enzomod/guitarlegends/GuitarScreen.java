package br.com.enzomod.guitarlegends;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class GuitarScreen extends Screen {
    private int song = 0;

    public GuitarScreen() {
        super(Text.literal("Minecraft Guitar Legends 3D"));
    }

    private void start3D() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(song);
        ClientPlayNetworking.send(RhythmNetwork.START_3D, buf);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.fill(0, 0, width, 52, 0xDD080D1B);
        context.fill(0, 50, width, 53, 0xFFFFB52B);
        context.drawCenteredTextWithShadow(textRenderer, "MINECRAFT GUITAR LEGENDS 3D", width / 2, 12, 0xFFFFD45C);
        context.drawCenteredTextWithShadow(textRenderer, "CARRINHO + 5 TRILHOS + F G H J K", width / 2, 31, 0xFF9EDBFF);

        context.drawCenteredTextWithShadow(textRenderer, "ESCOLHA A MUSICA", width / 2, 67, 0xFFFFFFFF);
        for (int i = 0; i < SongCharts.NAMES.length; i++) {
            int y = 91 + i * 19;
            int color = i == song ? 0xFFFFFF55 : 0xFFD9E4F5;
            String prefix = i == song ? ">  " : "   ";
            context.drawCenteredTextWithShadow(textRenderer, prefix + SongCharts.NAMES[i], width / 2, y, color);
        }

        int infoY = height - 63;
        context.drawCenteredTextWithShadow(textRenderer, "CIMA/BAIXO: escolher musica", width / 2, infoY, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "ENTER: construir arena e jogar em 3D", width / 2, infoY + 17, 0xFF80FF80);
        context.drawCenteredTextWithShadow(textRenderer, "No jogo: F=verde  G=vermelho  H=amarelo  J=azul  K=laranja", width / 2, infoY + 34, 0xFF9EDBFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_UP) {
            song = (song + SongCharts.NAMES.length - 1) % SongCharts.NAMES.length;
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            song = (song + 1) % SongCharts.NAMES.length;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            start3D();
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
