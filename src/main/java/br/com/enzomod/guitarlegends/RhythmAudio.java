package br.com.enzomod.guitarlegends;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class RhythmAudio {
    private RhythmAudio() {}

    private static int scheduledSong = -1;
    private static long scheduledAtMs = 0L;
    private static SoundInstance current;
    private static boolean gameActive = false;

    public static void schedule(int song, int delayMs) {
        stopSoundOnly();
        scheduledSong = song;
        scheduledAtMs = System.currentTimeMillis() + Math.max(0, delayMs);
        gameActive = true;
    }

    public static void tick(MinecraftClient client) {
        if (scheduledSong < 0 || System.currentTimeMillis() < scheduledAtMs) return;
        int song = scheduledSong;
        scheduledSong = -1;
        if (song < 0 || song >= SongCharts.IDS.length) return;
        Identifier id = new Identifier("guitarlegends", "song." + SongCharts.IDS[song]);
        current = PositionedSoundInstance.master(SoundEvent.of(id), 1.0f, 1.0f);
        client.getSoundManager().play(current);
    }

    public static boolean isGameActive() {
        return gameActive;
    }

    public static void stop() {
        scheduledSong = -1;
        scheduledAtMs = 0L;
        gameActive = false;
        stopSoundOnly();
    }

    private static void stopSoundOnly() {
        if (current != null) {
            MinecraftClient.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }
}
