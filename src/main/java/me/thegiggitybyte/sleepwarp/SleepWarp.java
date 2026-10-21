package me.thegiggitybyte.sleepwarp;

import me.thegiggitybyte.sleepwarp.config.SleepWarpConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(SleepWarp.MOD_ID)
public final class SleepWarp {
    public static final String MOD_ID = "sleepwarp";

    public SleepWarp(IEventBus modEventBus, ModContainer modContainer) {
        SleepWarpConfig.register(modEventBus, modContainer);
        Commands.register();
        WarpEngine.initialize();
    }
}
