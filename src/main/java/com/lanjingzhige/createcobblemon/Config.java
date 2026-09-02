package com.lanjingzhige.createcobblemon;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    public static final ModConfigSpec.BooleanValue ENABLE_SHULKER_TELEPORTER_CAMERA_OFFSET = BUILDER
            .comment("Apply the Shulker Teleporter first-person camera offset.")
            .define("enableShulkerTeleporterCameraOffset", true);

    public static final ModConfigSpec.BooleanValue ENABLE_SHULKER_TELEPORTER_PLAYER_CLIPPING = BUILDER
            .comment("Clip players/mobs inside a closing Shulker Teleporter shell.")
            .define("enableShulkerTeleporterPlayerClipping", true);

    public static final ShulkerTeleporter SHULKER_TELEPORTER = new ShulkerTeleporter(BUILDER);

    public static class ShulkerTeleporter {
        public final ModConfigSpec.BooleanValue allowCrossDimension;
        public final ModConfigSpec.BooleanValue allowDestinationChunkLoading;
        public final ModConfigSpec.DoubleValue maxSameSpaceDistance;
        public final ModConfigSpec.BooleanValue allowPlayers;
        public final ModConfigSpec.BooleanValue allowMobs;
        public final ModConfigSpec.BooleanValue allowItems;
        public final ModConfigSpec.IntValue maxEntitiesPerTeleport;
        public final ModConfigSpec.IntValue arrivalCooldownTicks;

        ShulkerTeleporter(ModConfigSpec.Builder builder) {
            builder.push("shulkerTeleporter");
            allowCrossDimension = builder.define("allowCrossDimension", true);
            allowDestinationChunkLoading = builder
                    .comment("Allow static-world destinations to load their chunk while resolving a teleport.")
                    .define("allowDestinationChunkLoading", true);
            maxSameSpaceDistance = builder
                    .comment("Maximum destination distance within the same dimension and sublevel. 0 means unlimited.")
                    .defineInRange("maxSameSpaceDistance", 0.0d, 0.0d, 30000000.0d);
            allowPlayers = builder.define("allowPlayers", true);
            allowMobs = builder.define("allowMobs", true);
            allowItems = builder.define("allowItems", true);
            maxEntitiesPerTeleport = builder
                    .comment("Maximum entities moved in one activation. 0 means unlimited.")
                    .defineInRange("maxEntitiesPerTeleport", 0, 0, 1024);
            arrivalCooldownTicks = builder.defineInRange("arrivalCooldownTicks", 80, 0, Integer.MAX_VALUE);
            builder.pop();
        }
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
