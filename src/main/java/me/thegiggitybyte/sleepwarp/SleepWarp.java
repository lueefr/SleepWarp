package me.thegiggitybyte.sleepwarp;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(SleepWarp.MOD_ID)
public final class SleepWarp {
    public static final String MOD_ID = "sleepwarp";

    public SleepWarp(ModContainer modContainer) {
        SleepWarpConfig.register(modContainer);
        Commands.register();
        WarpEngine.initialize();
    }
}
