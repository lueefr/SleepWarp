package me.thegiggitybyte.sleepwarp.mixin.common;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"
            )
    )
    private boolean sleepwarp$suppressVanillaNightSkip(SleepStatus sleepStatus, int percentage) {
        return false;
    }

    @Redirect(
            method = "updateSleepingPlayerList",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;announceSleepStatus()V"
            )
    )
    private void sleepwarp$sendWarpStatus(ServerLevel world) {
        if (world.getServer().isSingleplayer() || !world.getServer().isPublished() || world.players().size() == 1) {
            return;
        }
        if (!SleepWarpConfig.action_bar_messages) {
            return;
        }

        long playerCount = 0;
        long inBedCount = 0;
        long sleepingCount = 0;

        for (var player : world.players()) {
            if (player.isSleeping()) {
                if (player.getSleepTimer() >= 100) {
                    ++sleepingCount;
                }
                ++inBedCount;
            }
            ++playerCount;
        }

        Component messageText = null;
        var tallyText = Component.empty()
                .append(Component.literal(String.valueOf(inBedCount)))
                .append("/")
                .append(Component.literal(String.valueOf(playerCount)));

        if (inBedCount == 0) {
            messageText = Component.translatable("text.sleepwarp.players_sleeping", tallyText.withStyle(ChatFormatting.DARK_GRAY));
        } else if (SleepWarpConfig.use_sleep_percentage) {
            var percentRequired = world.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
            var minSleepingCount = Math.max(1, Mth.ceil((playerCount * percentRequired) / 100.0F));

            if (sleepingCount < minSleepingCount && minSleepingCount - inBedCount > 0) {
                messageText = Component.translatable(
                        "text.sleepwarp.players_sleeping.more_required",
                        tallyText.withStyle(ChatFormatting.RED),
                        String.valueOf(minSleepingCount - inBedCount)
                );
            } else {
                messageText = Component.translatable("text.sleepwarp.players_sleeping", tallyText.withStyle(ChatFormatting.DARK_GREEN));
            }
        } else if (sleepingCount == 0) {
            messageText = Component.translatable("text.sleepwarp.players_sleeping", tallyText.withStyle(ChatFormatting.YELLOW));
        }

        if (messageText != null) {
            for (var player : world.players()) {
                player.sendSystemMessage(messageText, true);
            }
        }
    }
}
