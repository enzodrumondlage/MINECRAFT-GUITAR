package br.com.enzomod.guitarlegends;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class GuitarScreen extends Screen {
    private static final int[] COLORS = {
        0xFF20D050, 0xFFE03030, 0xFFE0D030, 0xFF3080F0, 0xFFF08020
    };
    private static final String[] SONGS = {
        "Wuthering Heights", "Pense no Lula", "Solinho do Fora PT",
        "Sabor Picanha", "Mega Funk Fora PT", "Tropa do Capitao!"
    };

    /*
     * Original demo melodies, made from Minecraft's built-in harp sound.
     * These are playable backing patterns, not recordings of the named songs.
     * Each number selects one of the five rhythm lanes.
     */
    private static final int[][] MELODIES = {
        {0,2,4,2,1,2,3,2,0,2,4,3,2,1,2,3},
        {0,0,2,3,2,1,0,2,3,3,4,3,2,1,0,1},
        {0,2,1,3,2,4,3,2,0,2,3,4,2,1,0,2},
        {0,1,2,1,3,2,4,2,0,1,2,3,2,1,0,2},
        {0,0,3,2,4,2,3,1,0,2,4,3,2,3,1,0},
        {0,2,3,4,3,2,1,2,0,2,4,2,3,1,0,1}
    };
    private static final int[] SEMITONES = {0, 3, 5, 7, 10};

    private final Random random = new Random(123);
    private int song = 0, mode = 0, score = 0, combo = 0, misses = 0;
    private long start = 0;
    private boolean playing = false;
    private final int[] lanes = new int[220];
    private final boolean[] done = new boolean[220];
    private final boolean[] soundPlayed = new boolean[220];

    public GuitarScreen() {
        super(Text.literal("Guitar Legends"));
    }

    private long interval() {
        return 700L - mode * 140L;
    }

    private long targetTime(int note) {
        return 1800L + note * interval();
    }

    private void begin() {
        random.setSeed(song * 91L + mode * 31L);
        int[] melody = MELODIES[song];
        for (int i = 0; i < lanes.length; i++) {
            // Repeat the original melody with small, deterministic variations.
            int lane = melody[i % melody.length];
            if (i > 0 && i % 16 == 15 && random.nextBoolean()) {
                lane = random.nextInt(SEMITONES.length);
            }
            lanes[i] = lane;
            done[i] = false;
            soundPlayed[i] = false;
        }
        score = combo = misses = 0;
        playing = true;
        start = System.nanoTime();
    }

    private long time() {
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private void playNote(int noteIndex) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int lane = lanes[noteIndex];
        int semitones = SEMITONES[lane];
        // Keep the melody in a comfortable range and give each lane its own pitch.
        float pitch = (float) (0.72 * Math.pow(2.0, semitones / 12.0));
        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, 0.85f, pitch);
    }

    private void hit(int lane) {
        if (!playing) return;
        long t = time();
        int best = -1;
        long diff = 999999;
        for (int i = 0; i < lanes.length; i++) {
            long d = Math.abs(targetTime(i) - t);
            if (!done[i] && lanes[i] == lane && d < diff) {
                best = i;
                diff = d;
            }
        }
        if (best >= 0 && diff < 180) {
            done[best] = true;
            combo++;
            score += (diff < 70 ? 100 : diff < 120 ? 75 : 50) * Math.min(4, 1 + combo / 10);
        } else {
            miss();
        }
    }

    private void miss() {
        misses++;
        combo = 0;
        score = Math.max(0, score - 25);
        if ((mode == 1 && misses >= 20) || (mode == 2 && misses >= 10) || (mode == 3 && misses >= 1)) {
            playing = false;
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        renderBackground(c);
        c.drawCenteredTextWithShadow(textRenderer, "MINECRAFT GUITAR LEGENDS", width / 2, 15, 0xFFFFCC55);

        if (!playing) {
            c.drawCenteredTextWithShadow(textRenderer, "SELECIONE A MUSICA (CIMA/BAIXO)", width / 2, 45, 0xFFFFFFFF);
            for (int i = 0; i < SONGS.length; i++) {
                c.drawCenteredTextWithShadow(textRenderer, (i == song ? "> " : "") + SONGS[i],
                    width / 2, 70 + i * 17, i == song ? 0xFFFFFF55 : 0xFFFFFFFF);
            }
            c.drawCenteredTextWithShadow(textRenderer,
                "Dificuldade: " + new String[]{"Facil", "Medio", "Dificil", "Impossivel"}[mode] + " (ESQUERDA/DIREITA)",
                width / 2, 190, 0xFFFFFFFF);
            c.drawCenteredTextWithShadow(textRenderer, "ENTER jogar | SHIFT sair | DEMO MUSICAL ORIGINAL",
                width / 2, 215, 0xFF80FF80);
        } else {
            long t = time();
            int w = 40, left = width / 2 - 100, bottom = height - 58, top = 53;
            c.drawTextWithShadow(textRenderer, "Pontos: " + score + "  Combo: " + combo + "  Erros: " + misses,
                10, 35, 0xFFFFFFFF);

            for (int i = 0; i < 5; i++) {
                int x = left + i * w;
                c.fill(x, top, x + w - 2, bottom, 0xAA202030);
                c.fill(x, bottom - 3, x + w - 2, bottom + 30, COLORS[i]);
                c.drawCenteredTextWithShadow(textRenderer, "FGHJK".substring(i, i + 1),
                    x + w / 2 - 3, bottom + 8, 0xFFFFFFFF);
            }

            for (int i = 0; i < lanes.length; i++) {
                long dt = targetTime(i) - t;

                // Play each melody note once, on the exact beat used by the lane chart.
                if (!soundPlayed[i] && dt <= 0) {
                    soundPlayed[i] = true;
                    playNote(i);
                }

                if (done[i]) continue;
                if (dt < -180) {
                    done[i] = true;
                    miss();
                    continue;
                }
                if (dt > 1800) continue;

                int y = bottom - (int) (dt * (bottom - top) / 1800);
                int x = left + lanes[i] * w;
                c.fill(x + 3, y - 7, x + w - 5, y + 7, COLORS[lanes[i]]);
            }

            if (t > targetTime(lanes.length - 1) + 500) playing = false;
        }
        super.render(c, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        if (!playing) {
            if (key == GLFW.GLFW_KEY_UP) song = (song + SONGS.length - 1) % SONGS.length;
            else if (key == GLFW.GLFW_KEY_DOWN) song = (song + 1) % SONGS.length;
            else if (key == GLFW.GLFW_KEY_LEFT) mode = (mode + 3) % 4;
            else if (key == GLFW.GLFW_KEY_RIGHT) mode = (mode + 1) % 4;
            else if (key == GLFW.GLFW_KEY_ENTER) begin();
            else return super.keyPressed(key, scan, mods);
            return true;
        }

        int[] keys = {GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K};
        for (int i = 0; i < keys.length; i++) {
            if (key == keys[i]) {
                hit(i);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
