package me.thegiggitybyte.sleepwarp;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import me.thegiggitybyte.sleepwarp.runnable.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Handles incrementing time and simulating the world.
 */
public class WarpEngine {
    public static final int DAY_LENGTH_TICKS = 24000;
    private static WarpEngine instance;
    private final Random random;

    private WarpEngine() {
        random = new Random();
        NeoForge.EVENT_BUS.addListener(this::onLevelPostTick);
        NeoForge.EVENT_BUS.addListener(this::onCanPlayerSleep);
        NeoForge.EVENT_BUS.addListener(this::onCanContinueSleeping);
    }

    public static void initialize() {
        if (instance != null) throw new AssertionError();
        instance = new WarpEngine();
    }

    // Equivalent of the Fabric ALLOW_SLEEP_TIME hook: after dusk time 12542 vanilla may
    // already consider it "day" again (e.g. the dawn window close to tick 24000), but a
    // player getting into / staying in bed then should still count as sleeping so the
    // warp can finish and wake everyone up at day time 0.
    private void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW
                && event.getEntity().level().getDayTime() % DAY_LENGTH_TICKS > 12542) {
            event.setProblem(null);
        }
    }

    private void onCanContinueSleeping(CanContinueSleepingEvent event) {
        if (event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW
                && event.getEntity().level().getDayTime() % DAY_LENGTH_TICKS > 12542) {
            event.setContinueSleeping(true);
        }
    }

    private void onLevelPostTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel world) {
            onEndTick(world);
        }
    }

    private void onEndTick(ServerLevel world) {
        // Pre-warp checks.
        if (!world.canSleepThroughNights()) return;

        var totalPlayers = world.players().size();
        var sleepingPlayers = world.players().stream().filter(Player::isSleepingLongEnough).count();
        if (sleepingPlayers == 0) return;

        if (SleepWarpConfig.use_sleep_percentage) {
            var percentRequired = world.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
            var minimumSleeping = Math.max(1, Mth.ceil((totalPlayers * percentRequired) / 100.0F));
            if (sleepingPlayers < minimumSleeping) return;
        }

        // Determine amount of ticks to add to time.
        var maxTicksAdded = Math.max(10, SleepWarpConfig.max_ticks_added);
        var playerMultiplier = Math.max(0.05, Math.min(1.0, SleepWarpConfig.player_multiplier));
        var worldTime = world.getDayTime() % DAY_LENGTH_TICKS;
        int warpTickCount;

        if (worldTime + maxTicksAdded < DAY_LENGTH_TICKS) {
            if (totalPlayers == 1) {
                warpTickCount = maxTicksAdded;
            } else {
                var sleepingRatio = (double) sleepingPlayers / totalPlayers;
                var scaledRatio = sleepingRatio * playerMultiplier;
                var tickMultiplier = scaledRatio / ((scaledRatio * 2) - playerMultiplier - sleepingRatio + 1);

                warpTickCount = Math.toIntExact(Math.round(maxTicksAdded * tickMultiplier));
            }
        } else {
            warpTickCount = Math.toIntExact(DAY_LENGTH_TICKS % worldTime);
        }

        // Collect valid chunks to tick.
        var chunkMap = world.getChunkSource().chunkMap;
        var chunks = new ArrayList<LevelChunk>();

        for (ChunkHolder chunkHolder : chunkMap.getChunks()) {
            LevelChunk chunk = chunkHolder.getTickingChunk();

            if (chunk != null && world.isNaturalSpawningAllowed(chunk.getPos())
                    && chunkMap.anyPlayerCloseEnoughForSpawning(chunk.getPos())) {
                chunks.add(chunk);
            }
        }

        // Accelerate time and tick world.
        var doDaylightCycle = world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);

        for (var tick = 0; tick < warpTickCount; tick++) {
            world.advanceWeatherCycle();
            world.updateSkyBrightness();
            if (SleepWarpConfig.tick_game_time) {
                world.tickTime();
            } else {
                world.setDayTime(world.getDayTime() + 1L);
            }

            var packet = new ClientboundSetTimePacket(world.getGameTime(), world.getDayTime(), doDaylightCycle);
            world.getServer().getPlayerList().broadcastAll(packet, world.dimension());

            Collections.shuffle(chunks);
            for (var chunk : chunks) {
                if (SleepWarpConfig.tick_random_block) {
                    this.execute(world, new RandomTickRunnable(world, chunk));
                }
                if (world.isRaining()) {
                    if (SleepWarpConfig.tick_lightning && world.isThundering() && random.nextInt(100000) == 0) {
                        this.execute(world, new LightningTickRunnable(world, chunk));
                    }

                    if (random.nextInt(16) == 0) {
                        this.execute(world, new PrecipitationTickRunnable(world, chunk));
                    }
                }
            }

            if (SleepWarpConfig.tick_block_entities) {
                this.execute(world, new BlockTickRunnable(world));
            }
        }

        if (SleepWarpConfig.tick_animals | SleepWarpConfig.tick_monsters) {
            this.execute(world, new MobTickRunnable(world, warpTickCount));
        }

        worldTime = world.getDayTime() % DAY_LENGTH_TICKS;
        MutableComponent actionBarText = null;

        if (worldTime == 0) {
            if (world.isRaining()) world.resetWeatherCycle();
            world.wakeUpAllPlayers();

            var currentDay = String.valueOf(world.getDayTime() / DAY_LENGTH_TICKS);
            actionBarText = Component.translatable("text.sleepwarp.day", Component.literal(currentDay).withStyle(ChatFormatting.GOLD));
        } else if (worldTime > 0) {
            var remainingTicks = world.isThundering()
                    ? world.serverLevelData.getThunderTime()
                    : DAY_LENGTH_TICKS - worldTime;

            if (remainingTicks > 0) {
                actionBarText = Component.empty();
                if (totalPlayers > 1) {
                    var requiredPercentage = 1.0 - playerMultiplier;
                    var actualPercentage = (double) sleepingPlayers / totalPlayers;
                    var indicatorColor = actualPercentage >= requiredPercentage ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
                    var playerNoun = (sleepingPlayers == 1 ? "player" : "players");
                    actionBarText.append(Component.translatable("text.sleepwarp." + playerNoun + "_sleeping", Component.literal("⌛ " + sleepingPlayers + ' ').withStyle(indicatorColor)));
                } else {
                    actionBarText.append(Component.literal("⌛").withStyle(ChatFormatting.GOLD));
                }

                var remainingSeconds = Math.round(((double) remainingTicks / Math.max(1, warpTickCount)) / 20);
                actionBarText.append(CommonComponents.space());
                actionBarText.append(Component.translatable("text.sleepwarp.until_" + (world.isThundering() ? "thunderstorm" : "dawn"), Component.literal(String.valueOf(remainingSeconds))));
            }
        }

        if (SleepWarpConfig.action_bar_messages && actionBarText != null) {
            for (var player : world.players()) {
                player.sendSystemMessage(actionBarText, true);
            }
        }
    }

    private void execute(ServerLevel world, Runnable runnable) {
        //CompletableFuture.runAsync(runnable);
        world.getServer().execute(runnable);
    }
}
