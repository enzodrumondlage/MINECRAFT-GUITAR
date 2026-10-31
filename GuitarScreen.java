package br.com.enzomod.guitarlegends;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GuitarScreen extends Screen {
    private static final int[] COLORS = {0xFF27CF55,0xFFEF4545,0xFFF2D33B,0xFF3E95F5,0xFFFF942F};
    private static final String[] KEYS = {"F","G","H","J","K"};
    private static final int[] GLFW_KEYS = {GLFW.GLFW_KEY_F,GLFW.GLFW_KEY_G,GLFW.GLFW_KEY_H,GLFW.GLFW_KEY_J,GLFW.GLFW_KEY_K};
    private static final long BEAT_MS = 500L; // base do prototipo; cada musica tera seu mapa proprio
    private final List<Note> notes = new ArrayList<>();
    private final Random random = new Random(42);
    private long startTime;
    private long lastSpawn;
    private int score;
    private int combo;
    private int maxCombo;
    private int perfect;
    private int hits;
    private int misses;

    public GuitarScreen() { super(Text.literal("Minecraft Guitar Legends")); }

    @Override protected void init() {
        startTime = System.currentTimeMillis();
        lastSpawn = startTime - BEAT_MS;
        score = combo = maxCombo = perfect = hits = misses = 0;
        notes.clear();
    }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        long now = System.currentTimeMillis();
        while (now - lastSpawn >= BEAT_MS) {
            lastSpawn += BEAT_MS;
            notes.add(new Note(random.nextInt(5), lastSpawn));
        }

        int laneW = 42;
        int left = width / 2 - (laneW * 5) / 2;
        int top = 55;
        int hitY = height - 82;
        int travel = Math.max(1, hitY - top);

        ctx.drawCenteredTextWithShadow(textRenderer, "MINECRAFT GUITAR LEGENDS", width/2, 14, 0xFFFFDD66);
        ctx.drawTextWithShadow(textRenderer, "Pontos: " + score, 12, 14, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Combo: x" + combo, 12, 28, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Recorde combo: x" + maxCombo, 12, 42, 0xFFFFFFFF);

        for (int i=0;i<5;i++) {
            int x1=left+i*laneW, x2=x1+laneW-2;
            ctx.fill(x1, top, x2, hitY+30, 0xBB171720);
            ctx.fill(x1, hitY, x2, hitY+30, COLORS[i]);
            ctx.drawCenteredTextWithShadow(textRenderer, KEYS[i], x1+laneW/2, hitY+10, 0xFFFFFFFF);
        }

        Iterator<Note> it = notes.iterator();
        while (it.hasNext()) {
            Note n = it.next();
            long age = now - n.time;
            float progress = (age + 1600f) / 1600f;
            int y = top + Math.round(progress * travel);
            if (y > hitY + 36) {
                misses++; combo=0; it.remove(); continue;
            }
            int x1=left+n.lane*laneW+5;
            ctx.fill(x1, y, x1+laneW-12, y+12, COLORS[n.lane]);
        }

        ctx.drawCenteredTextWithShadow(textRenderer, "F G H J K no ritmo  |  SHIFT sai", width/2, height-24, 0xFFFFFFFF);
        super.render(ctx,mx,my,delta);
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key==GLFW.GLFW_KEY_LEFT_SHIFT || key==GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        for (int lane=0; lane<5; lane++) if (key==GLFW_KEYS[lane]) { hit(lane); return true; }
        return super.keyPressed(key,scan,mods);
    }

    private void hit(int lane) {
        long now=System.currentTimeMillis();
        Note best=null; long bestDiff=Long.MAX_VALUE;
        for (Note n:notes) if(n.lane==lane) {
            long target=n.time+1600L;
            long d=Math.abs(now-target);
            if(d<bestDiff){best=n;bestDiff=d;}
        }
        if(best!=null && bestDiff<=180) {
            notes.remove(best); hits++; combo++; maxCombo=Math.max(maxCombo,combo);
            if(bestDiff<=70){perfect++; score+=1000+combo*10;} else score+=500+combo*5;
        } else { misses++; combo=0; }
    }

    @Override public boolean shouldPause(){ return false; }
    private static class Note { final int lane; final long time; Note(int lane,long time){this.lane=lane;this.time=time;} }
}
