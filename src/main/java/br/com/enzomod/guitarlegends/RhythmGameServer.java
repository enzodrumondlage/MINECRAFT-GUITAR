package br.com.enzomod.guitarlegends;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RhythmGameServer {
    private RhythmGameServer() {}

    private static final double BLOCKS_PER_SECOND = 3.0;
    private static final int START_DELAY_MS = 900;
    private static final int HIT_WINDOW_MS = 190;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final BlockState RAIL = Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.NORTH_SOUTH);
    private static final BlockState[] LANE_BLOCKS = {
        Blocks.LIME_CONCRETE.getDefaultState(),
        Blocks.RED_CONCRETE.getDefaultState(),
        Blocks.YELLOW_CONCRETE.getDefaultState(),
        Blocks.BLUE_CONCRETE.getDefaultState(),
        Blocks.ORANGE_CONCRETE.getDefaultState()
    };
    private static final String[] LANE_KEYS = {"F", "G", "H", "J", "K"};

    public static void start(ServerPlayerEntity player, int song) {
        if (song < 0 || song >= SongCharts.NAMES.length) return;
        Session old = SESSIONS.remove(player.getUuid());
        if (old != null) old.end(true);

        ServerWorld world = player.getServerWorld();
        int top = world.getTopY() - 12;
        int arenaY = Math.max(world.getBottomY() + 20, Math.min(top, player.getBlockY() + 35));
        int baseX = player.getBlockX() - 6;
        int baseZ = player.getBlockZ() + 12;
        int length = 22 + (int)Math.ceil((SongCharts.DURATIONS_MS[song] / 1000.0) * BLOCKS_PER_SECOND);

        buildArena(world, song, baseX, arenaY, baseZ, length);

        MinecartEntity[] carts = new MinecartEntity[5];
        double startZ = baseZ + 5.5;
        for (int lane = 0; lane < 5; lane++) {
            MinecartEntity cart = EntityType.MINECART.create(world);
            if (cart == null) {
                for (MinecartEntity spawned : carts) if (spawned != null) spawned.discard();
                return;
            }
            cart.refreshPositionAndAngles(laneX(baseX, lane) + 0.5, arenaY + 1.05, startZ, 0.0f, 0.0f);
            cart.setNoGravity(true);
            world.spawnEntity(cart);
            carts[lane] = cart;
        }

        int startLane = 2;
        Session session = new Session(player, carts, song, baseX, arenaY, baseZ, startLane,
            new Vec3d(player.getX(), player.getY(), player.getZ()), player.getYaw(), player.getPitch());
        SESSIONS.put(player.getUuid(), session);
        player.startRiding(carts[startLane], true);
        player.sendMessage(Text.literal("Minecraft Guitar Legends 3D: F G H J K fazem o Steve trocar de carrinho. Segure notas longas!"), false);
        RhythmNetwork.playAudio(player, song, START_DELAY_MS);
    }

    public static void onLaneInput(ServerPlayerEntity player, int lane, boolean pressed) {
        Session session = SESSIONS.get(player.getUuid());
        if (session == null || lane < 0 || lane > 4) return;
        session.input(lane, pressed);
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Session session = it.next().getValue();
            if (session.tick()) {
                session.end(false);
                it.remove();
            }
        }
    }

    private static int laneX(int baseX, int lane) {
        return baseX + lane * 3;
    }

    private static int holdDuration(int song, int index) {
        int[] times = SongCharts.TIMES_MS[song];
        int start = times[index];
        int next = index + 1 < times.length ? times[index + 1] : SongCharts.DURATIONS_MS[song];
        int gap = Math.max(0, next - start);
        if (gap < 650) return 0;
        if (gap < 1050 && ((index + song) & 1) != 0) return 0;
        return Math.min(2200, Math.max(450, gap - 170));
    }

    private static void buildArena(ServerWorld world, int song, int baseX, int y, int baseZ, int length) {
        int minX = baseX - 4;
        int maxX = baseX + 16;
        int endZ = baseZ + length;
        BlockState black = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState wall = Blocks.GRAY_CONCRETE.getDefaultState();
        BlockState accent = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();

        for (int z = baseZ; z <= endZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                world.setBlockState(new BlockPos(x, y, z), black, 3);
            }
            world.setBlockState(new BlockPos(minX, y + 1, z), wall, 3);
            world.setBlockState(new BlockPos(minX, y + 2, z), wall, 3);
            world.setBlockState(new BlockPos(maxX, y + 1, z), wall, 3);
            world.setBlockState(new BlockPos(maxX, y + 2, z), wall, 3);
            if ((z - baseZ) % 8 == 0) {
                for (int h = 3; h <= 6; h++) {
                    world.setBlockState(new BlockPos(minX, y + h, z), accent, 3);
                    world.setBlockState(new BlockPos(maxX, y + h, z), accent, 3);
                }
                world.setBlockState(new BlockPos(minX + 1, y + 3, z), accent, 3);
                world.setBlockState(new BlockPos(maxX - 1, y + 3, z), accent, 3);
            }
        }

        for (int lane = 0; lane < 5; lane++) {
            int x = laneX(baseX, lane);
            for (int z = baseZ; z <= endZ; z++) {
                world.setBlockState(new BlockPos(x, y + 1, z), RAIL, 3);
            }
            for (int z = baseZ; z < baseZ + 5; z++) {
                paintLane(world, x, y, z, LANE_BLOCKS[lane]);
            }
        }

        int[] times = SongCharts.TIMES_MS[song];
        byte[] lanes = SongCharts.LANES[song];
        for (int i = 0; i < times.length; i++) {
            int lane = lanes[i];
            int x = laneX(baseX, lane);
            int z0 = baseZ + 5 + (int)Math.round((times[i] / 1000.0) * BLOCKS_PER_SECOND);
            int hold = holdDuration(song, i);
            int blocks = hold > 0 ? Math.max(2, (int)Math.ceil((hold / 1000.0) * BLOCKS_PER_SECOND)) : 1;
            for (int dz = 0; dz < blocks && z0 + dz <= endZ; dz++) {
                paintLane(world, x, y, z0 + dz, LANE_BLOCKS[lane]);
            }
        }

        for (int lane = 0; lane < 5; lane++) {
            int x = laneX(baseX, lane);
            for (int z = baseZ; z <= endZ; z++) {
                world.setBlockState(new BlockPos(x, y + 1, z), RAIL, 3);
            }
        }
    }

    private static void paintLane(ServerWorld world, int x, int y, int z, BlockState state) {
        world.setBlockState(new BlockPos(x - 1, y, z), state, 3);
        world.setBlockState(new BlockPos(x, y, z), state, 3);
        world.setBlockState(new BlockPos(x + 1, y, z), state, 3);
    }

    private static final class Session {
        final ServerPlayerEntity player;
        final MinecartEntity[] carts;
        final int song;
        final int baseX;
        final int arenaY;
        final int baseZ;
        final Vec3d origin;
        final float originYaw;
        final float originPitch;
        final long startAtMs;
        final boolean[] resolved;
        final boolean[] held = new boolean[5];
        int lane;
        int score;
        int combo;
        int bestCombo;
        int misses;
        int perfects;
        int hits;
        int activeHold = -1;
        int hudTicks;

        Session(ServerPlayerEntity player, MinecartEntity[] carts, int song, int baseX, int arenaY, int baseZ,
                int lane, Vec3d origin, float yaw, float pitch) {
            this.player = player;
            this.carts = carts;
            this.song = song;
            this.baseX = baseX;
            this.arenaY = arenaY;
            this.baseZ = baseZ;
            this.lane = lane;
            this.origin = origin;
            this.originYaw = yaw;
            this.originPitch = pitch;
            this.startAtMs = System.currentTimeMillis() + START_DELAY_MS;
            this.resolved = new boolean[SongCharts.TIMES_MS[song].length];
        }

        long elapsed() {
            return System.currentTimeMillis() - startAtMs;
        }

        void switchCart(int newLane) {
            MinecartEntity target = carts[newLane];
            if (target == null || target.isRemoved()) return;
            if (player.getVehicle() != target) {
                player.stopRiding();
                player.startRiding(target, true);
            }
        }

        void input(int newLane, boolean pressed) {
            long t = elapsed();
            if (t < -50) return;
            held[newLane] = pressed;
            if (!pressed) {
                if (activeHold >= 0 && SongCharts.LANES[song][activeHold] == newLane) {
                    long end = SongCharts.TIMES_MS[song][activeHold] + holdDuration(song, activeHold);
                    if (t < end - 80) failHold();
                }
                return;
            }

            if (activeHold >= 0 && SongCharts.LANES[song][activeHold] != newLane) failHold();
            lane = newLane;
            switchCart(newLane);

            int[] times = SongCharts.TIMES_MS[song];
            byte[] lanes = SongCharts.LANES[song];
            int best = -1;
            long bestDiff = Long.MAX_VALUE;
            for (int i = 0; i < times.length; i++) {
                if (resolved[i] || i == activeHold || lanes[i] != newLane) continue;
                long d = Math.abs(times[i] - t);
                if (d < bestDiff) {
                    best = i;
                    bestDiff = d;
                }
            }

            if (best >= 0 && bestDiff <= HIT_WINDOW_MS) {
                hits++;
                if (bestDiff <= 70) perfects++;
                int hold = holdDuration(song, best);
                if (hold > 0) {
                    activeHold = best;
                    score += bestDiff <= 70 ? 100 : 65;
                } else {
                    resolved[best] = true;
                    combo++;
                    bestCombo = Math.max(bestCombo, combo);
                    score += (bestDiff <= 70 ? 100 : 65) * multiplier();
                }
            } else {
                miss();
            }
        }

        boolean tick() {
            if (player.isRemoved()) return true;
            for (MinecartEntity cart : carts) if (cart == null || cart.isRemoved()) return true;

            long t = elapsed();
            double songTime = Math.max(0L, t) / 1000.0;
            double z = baseZ + 5.5 + songTime * BLOCKS_PER_SECOND;
            for (int i = 0; i < carts.length; i++) {
                MinecartEntity cart = carts[i];
                cart.requestTeleport(laneX(baseX, i) + 0.5, arenaY + 1.05, z);
                cart.setVelocity(0.0, 0.0, 0.0);
            }
            if (player.getVehicle() != carts[lane]) player.startRiding(carts[lane], true);

            if (t >= 0) {
                updateHold(t);
                updateMisses(t);
            }

            if (++hudTicks >= 8) {
                hudTicks = 0;
                player.sendMessage(Text.literal("🎸 " + SongCharts.NAMES[song] + "  |  Carrinho " + LANE_KEYS[lane] +
                    "  |  Pontos " + score + "  Combo " + combo + "  Erros " + misses), true);
            }
            return t >= SongCharts.DURATIONS_MS[song] + 500L;
        }

        void updateHold(long t) {
            if (activeHold < 0) return;
            int holdLane = SongCharts.LANES[song][activeHold];
            long end = SongCharts.TIMES_MS[song][activeHold] + holdDuration(song, activeHold);
            if (lane != holdLane || !held[holdLane]) {
                failHold();
                return;
            }
            if (t >= end) {
                int duration = holdDuration(song, activeHold);
                resolved[activeHold] = true;
                activeHold = -1;
                combo++;
                bestCombo = Math.max(bestCombo, combo);
                score += Math.max(80, duration / 8) * multiplier();
            }
        }

        void updateMisses(long t) {
            int[] times = SongCharts.TIMES_MS[song];
            for (int i = 0; i < times.length; i++) {
                if (resolved[i] || i == activeHold) continue;
                if (t - times[i] > HIT_WINDOW_MS) {
                    resolved[i] = true;
                    miss();
                }
            }
        }

        void failHold() {
            if (activeHold < 0) return;
            resolved[activeHold] = true;
            activeHold = -1;
            miss();
        }

        int multiplier() {
            return Math.min(4, 1 + combo / 10);
        }

        void miss() {
            misses++;
            combo = 0;
            score = Math.max(0, score - 25);
        }

        void end(boolean interrupted) {
            RhythmNetwork.stopAudio(player);
            if (!player.isRemoved()) {
                player.stopRiding();
                player.teleport(player.getServerWorld(), origin.x, origin.y, origin.z, originYaw, originPitch);
                if (!interrupted) {
                    player.sendMessage(Text.literal("Fim! Pontos: " + score + " | Perfect: " + perfects +
                        " | Acertos: " + hits + " | Erros: " + misses + " | Maior combo: " + bestCombo), false);
                }
            }
            for (MinecartEntity cart : carts) if (cart != null && !cart.isRemoved()) cart.discard();
        }
    }
}
