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
    private static final int[][] MELODIES = {
        {0,2,4,2,1,2,3,2,0,2,4,3,2,1,2,3},
        {0,0,2,3,2,1,0,2,3,3,4,3,2,1,0,1},
        {0,2,1,3,2,4,3,2,0,2,3,4,2,1,0,2},
        {0,1,2,1,3,2,4,2,0,1,2,3,2,1,0,2},
        {0,0,3,2,4,2,3,1,0,2,4,3,2,3,1,0},
        {0,2,3,4,3,2,1,2,0,2,4,2,3,1,0,1}
    };
    private static final int[] SEMITONES = {0, 3, 5, 7, 10};
    private static final String[] KEYS = {"F", "G", "H", "J", "K"};

    private final Random random = new Random(123);
    private int song = 0, mode = 0, score = 0, combo = 0, misses = 0;
    private long start = 0;
    private boolean playing = false;
    private final int[] lanes = new int[220];
    private final boolean[] done = new boolean[220];
    private final boolean[] soundPlayed = new boolean[220];

    public GuitarScreen() {
        super(Text.literal("Minecraft Guitar Legends"));
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
        int semitones = SEMITONES[lanes[noteIndex]];
        float pitch = (float) (0.72 * Math.pow(2.0, semitones / 12.0));
        client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 0.85f, pitch);
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

    private void drawHighway(DrawContext c, long t) {
        int highwayWidth = Math.min(500, width - 28);
        int laneWidth = highwayWidth / 5;
        int left = (width - laneWidth * 5) / 2;
        int top = 78;
        int bottom = height - 82;
        int hitLine = bottom - 3;

        // Outer frame and five dark, separated note lanes.
        c.fill(left - 5, top - 5, left + laneWidth * 5 + 5, bottom + 43, 0xDD080D1B);
        c.fill(left - 3, top - 3, left + laneWidth * 5 + 3, bottom + 41, 0xFF29354B);
        for (int lane = 0; lane < 5; lane++) {
            int x = left + lane * laneWidth;
            c.fill(x + 1, top, x + laneWidth - 2, bottom + 35, 0xCC111827);
            c.fill(x + 1, top, x + 3, bottom + 35, 0xFF263248);
            c.fill(x + laneWidth - 5, top, x + laneWidth - 3, bottom + 35, 0xFF263248);

            // Small cross-ties make every lane read like a musical rail.
            for (int y = top + 12; y < bottom + 32; y += 34) {
                c.fill(x + 5, y, x + laneWidth - 7, y + 2, 0x553F526D);
            }

            // Colored lane marker at the top.
            c.fill(x + 5, top + 5, x + laneWidth - 7, top + 10, COLORS[lane]);
        }

        // The hit line is shared by all five tracks.
        c.fill(left - 9, hitLine, left + laneWidth * 5 + 9, hitLine + 4, 0xFFEAF6FF);
        c.fill(left - 9, hitLine - 2, left + laneWidth * 5 + 9, hitLine, 0xFF536D8C);

        // Falling notes: glow, colored body, and a bright center.
        if (playing) {
            for (int i = 0; i < lanes.length; i++) {
                long dt = targetTime(i) - t;
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

                int lane = lanes[i];
                int centerX = left + lane * laneWidth + laneWidth / 2;
                int y = bottom - (int) (dt * (bottom - top) / 1800);
                int noteWidth = Math.max(15, Math.min(30, laneWidth - 12));
                int half = noteWidth / 2;
                c.fill(centerX - half - 4, y - 11, centerX + half + 4, y + 11, 0x553FFFFF);
                c.fill(centerX - half, y - 8, centerX + half, y + 8, COLORS[lane]);
                c.fill(centerX - half + 4, y - 5, centerX + half - 4, y - 2, 0xCCFFFFFF);
                c.fill(centerX - half + 3, y + 4, centerX + half - 3, y + 6, 0xAA101010);
            }
        }

        // Large colored fret pads and key labels below the hit line.
        for (int lane = 0; lane < 5; lane++) {
            int centerX = left + lane * laneWidth + laneWidth / 2;
            int padWidth = Math.min(36, laneWidth - 10);
            int half = padWidth / 2;
            c.fill(centerX - half - 2, bottom + 7, centerX + half + 2, bottom + 35, 0xFF05070D);
            c.fill(centerX - half, bottom + 9, centerX + half, bottom + 33, COLORS[lane]);
            c.fill(centerX - half + 4, bottom + 12, centerX + half - 4, bottom + 15, 0xCCFFFFFF);
            c.drawCenteredTextWithShadow(textRenderer, KEYS[lane], centerX, bottom + 18, 0xFFFFFFFF);
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        renderBackground(c);
        c.fill(0, 0, width, 48, 0xDD080D1B);
        c.fill(0, 47, width, 49, 0xFF40516E);
        c.drawCenteredTextWithShadow(textRenderer, "MINECRAFT GUITAR LEGENDS", width / 2, 8, 0xFFFFCC55);
        c.drawCenteredTextWithShadow(textRenderer, "GUITAR  -  SISTEMA MUSICAL  -  GUITAR", width / 2, 27, 0xFF9EDBFF);

        if (!playing) {
            c.drawCenteredTextWithShadow(textRenderer, "ESCOLHA UMA FAIXA  (CIMA / BAIXO)", width / 2, 57, 0xFFFFFFFF);
            for (int i = 0; i < SONGS.length; i++) {
                int y = 82 + i * 16;
                if (y > height - 70) break;
                c.drawCenteredTextWithShadow(textRenderer, (i == song ? ">  " : "") + SONGS[i],
                    width / 2, y, i == song ? 0xFFFFFF55 : 0xFFD9E4F5);
            }
            c.drawCenteredTextWithShadow(textRenderer,
                "DIFICULDADE: " + new String[]{"FACIL", "MEDIO", "DIFICIL", "IMPOSSIVEL"}[mode] + "  (ESQUERDA / DIREITA)",
                width / 2, height - 45, 0xFFFFFFFF);
            c.drawCenteredTextWithShadow(textRenderer, "ENTER: JOGAR     SHIFT: SAIR", width / 2, height - 28, 0xFF80FF80);
        } else {
            c.drawTextWithShadow(textRenderer, "PONTOS " + score, 10, 56, 0xFFFFFFFF);
            c.drawTextWithShadow(textRenderer, "COMBO " + combo, 10, 69, 0xFFFFD65C);
            c.drawTextWithShadow(textRenderer, "ERROS " + misses, Math.max(10, width - 80), 56, 0xFFFF7777);
            drawHighway(c, time());
            if (!playing) {
                c.fill(width / 2 - 75, height / 2 - 18, width / 2 + 75, height / 2 + 18, 0xEE080D1B);
                c.drawCenteredTextWithShadow(textRenderer, "FIM DA MUSICA", width / 2, height / 2 - 5, 0xFFFFD65C);
                c.drawCenteredTextWithShadow(textRenderer, "SHIFT: VOLTAR", width / 2, height / 2 + 9, 0xFFFFFFFF);
            }
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
