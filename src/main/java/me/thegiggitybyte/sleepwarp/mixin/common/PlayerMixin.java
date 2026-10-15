package me.thegiggitybyte.sleepwarp.mixin.common;

import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"
            )
    )
    private boolean sleepwarp$allowSleepingInTheDay(BedRule bedRule, Level level) {
        Player player = (Player) (Object) this;
        if (bedRule.canSleep() == BedRule.Rule.WHEN_DARK && player.isSleeping()) {
            return true;
        }
        return bedRule.canSleep(level);
    }
}
