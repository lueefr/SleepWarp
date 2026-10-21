package me.thegiggitybyte.sleepwarp.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server/common configuration for SleepWarp.
 *
 * Values are copied into plain fields after load/reload so the gameplay code can
 * continue to read the settings without depending on the configuration library.
 */
public final class SleepWarpConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ACTION_BAR_MESSAGES;
    private static final ModConfigSpec.BooleanValue USE_SLEEP_PERCENTAGE;
    private static final ModConfigSpec.BooleanValue TICK_GAME_TIME;
    private static final ModConfigSpec.BooleanValue TICK_BLOCK_ENTITIES;
    private static final ModConfigSpec.BooleanValue TICK_RANDOM_BLOCK;
    private static final ModConfigSpec.BooleanValue TICK_SNOW_ACCUMULATION;
    private static final ModConfigSpec.BooleanValue TICK_ICE_FREEZING;
    private static final ModConfigSpec.BooleanValue TICK_LIGHTNING;
    private static final ModConfigSpec.BooleanValue TICK_MONSTERS;
    private static final ModConfigSpec.BooleanValue TICK_ANIMALS;
    private static final ModConfigSpec.IntValue MAX_TICKS_ADDED;
    private static final ModConfigSpec.DoubleValue PLAYER_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue MONSTER_TICK_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue ANIMAL_TICK_MULTIPLIER;
    private static final ModConfigSpec.BooleanValue LOG_ERROR_MESSAGES;

    public static final ModConfigSpec SPEC;

    public static boolean action_bar_messages = true;
    public static boolean use_sleep_percentage = false;
    public static boolean tick_game_time = false;
    public static boolean tick_block_entities = false;
    public static boolean tick_random_block = true;
    public static boolean tick_snow_accumulation = true;
    public static boolean tick_ice_freezing = true;
    public static boolean tick_lightning = true;
    public static boolean tick_monsters = false;
    public static boolean tick_animals = false;
    public static int max_ticks_added = 40;
    public static double player_multiplier = 0.6d;
    public static double monster_tick_multiplier = 0.25d;
    public static double animal_tick_multiplier = 0.25d;
    public static boolean log_error_messages = false;

    static {
        BUILDER.push("features");
        ACTION_BAR_MESSAGES = BUILDER
                .comment("Show SleepWarp status and progress in the action bar.")
                .define("action_bar_messages", true);
        USE_SLEEP_PERCENTAGE = BUILDER
                .comment("Use the playersSleepingPercentage gamerule before accelerating time.")
                .define("use_sleep_percentage", false);
        TICK_GAME_TIME = BUILDER.comment("Advance game time for each accelerated tick.").define("tick_game_time", false);
        TICK_BLOCK_ENTITIES = BUILDER.comment("Tick block entities, such as furnaces, during sleep.").define("tick_block_entities", false);
        TICK_RANDOM_BLOCK = BUILDER.comment("Run random block and fluid ticks, such as crop growth.").define("tick_random_block", true);
        TICK_SNOW_ACCUMULATION = BUILDER.comment("Allow snow to accumulate during accelerated time.").define("tick_snow_accumulation", true);
        TICK_ICE_FREEZING = BUILDER.comment("Allow water to freeze during accelerated time.").define("tick_ice_freezing", true);
        TICK_LIGHTNING = BUILDER.comment("Allow lightning to strike during accelerated time.").define("tick_lightning", true);
        TICK_MONSTERS = BUILDER.comment("Tick monsters during accelerated time.").define("tick_monsters", false);
        TICK_ANIMALS = BUILDER.comment("Tick animals during accelerated time.").define("tick_animals", false);
        BUILDER.pop();

        BUILDER.push("tuning");
        MAX_TICKS_ADDED = BUILDER
                .comment("Maximum world-clock ticks added for each server tick while sleeping.")
                .defineInRange("max_ticks_added", 40, 0, Integer.MAX_VALUE);
        PLAYER_MULTIPLIER = BUILDER
                .comment("Scales sleep acceleration with the share of players asleep.")
                .defineInRange("player_multiplier", 0.6d, 0.0d, 2.0d);
        MONSTER_TICK_MULTIPLIER = BUILDER
                .comment("Additional tick rate for monsters while sleeping.")
                .defineInRange("monster_tick_multiplier", 0.25d, 0.0d, 2.0d);
        ANIMAL_TICK_MULTIPLIER = BUILDER
                .comment("Additional tick rate for animals while sleeping.")
                .defineInRange("animal_tick_multiplier", 0.25d, 0.0d, 2.0d);
        LOG_ERROR_MESSAGES = BUILDER
                .comment("Log exceptions from optional block-entity ticking.")
                .define("log_error_messages", false);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private SleepWarpConfig() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(SleepWarpConfig::onConfigChanged);
        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC);
        // Plain fields above already hold the spec defaults; onConfigChanged
        // copies the actual file values once the config has been loaded.
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            copyValues();
        }
    }

    private static void copyValues() {
        action_bar_messages = ACTION_BAR_MESSAGES.get();
        use_sleep_percentage = USE_SLEEP_PERCENTAGE.get();
        tick_game_time = TICK_GAME_TIME.get();
        tick_block_entities = TICK_BLOCK_ENTITIES.get();
        tick_random_block = TICK_RANDOM_BLOCK.get();
        tick_snow_accumulation = TICK_SNOW_ACCUMULATION.get();
        tick_ice_freezing = TICK_ICE_FREEZING.get();
        tick_lightning = TICK_LIGHTNING.get();
        tick_monsters = TICK_MONSTERS.get();
        tick_animals = TICK_ANIMALS.get();
        max_ticks_added = MAX_TICKS_ADDED.get();
        player_multiplier = PLAYER_MULTIPLIER.get();
        monster_tick_multiplier = MONSTER_TICK_MULTIPLIER.get();
        animal_tick_multiplier = ANIMAL_TICK_MULTIPLIER.get();
        log_error_messages = LOG_ERROR_MESSAGES.get();
    }
}
