package com.lanjingzhige.createcobblemon.recipe.timemachine;

import com.lanjingzhige.createcobblemon.block.TimeMachineLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * "createcobblemon:time_machine" 配方的序列化器。
 * <p>
 * JSON 格式（放在 data/createcobblemon/recipe/time_machine/ 下）：
 * <pre>
 * {
 *   "type": "createcobblemon:time_machine",
 *   "inputs": [ { "item": "minecraft:apple" }, ... ],  // 最多 6 个，槽位可少可空
 *   "species": "cobblemon:pikachu",                    // 召唤的宝可梦物种
 *   "level": 70                                        // 等级，默认 70
 * }
 * </pre>
 */
public class TimeMachineRecipeSerializer implements RecipeSerializer<TimeMachineRecipe> {

    /** 输入最多 6 个槽位 */
    private static final Codec<List<Ingredient>> INPUTS_CODEC = Ingredient.CODEC.listOf().flatXmap(
        list -> list.size() > TimeMachineLayout.ITEM_ARM_COUNT
            ? DataResult.error(() -> "Time machine recipes take at most " + TimeMachineLayout.ITEM_ARM_COUNT + " inputs")
            : DataResult.success(list),
        DataResult::success
    );

    private static final MapCodec<TimeMachineRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            INPUTS_CODEC.optionalFieldOf("inputs", List.of()).forGetter(TimeMachineRecipe::getInputs),
            ResourceLocation.CODEC.fieldOf("species").forGetter(TimeMachineRecipe::getSpeciesId),
            Codec.INT.optionalFieldOf("level", 70).forGetter(TimeMachineRecipe::getLevel)
        ).apply(instance, TimeMachineRecipe::new));

    /** 网络传输（例如配方同步给客户端）用 */
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Ingredient>> INPUTS_STREAM =
        ByteBufCodecs.collection(ArrayList::new, Ingredient.CONTENTS_STREAM_CODEC);

    private static final StreamCodec<RegistryFriendlyByteBuf, TimeMachineRecipe> STREAM = StreamCodec.composite(
            INPUTS_STREAM, TimeMachineRecipe::getInputs,
            ResourceLocation.STREAM_CODEC, TimeMachineRecipe::getSpeciesId,
            ByteBufCodecs.VAR_INT, TimeMachineRecipe::getLevel,
            TimeMachineRecipe::new);

    @Override
    public MapCodec<TimeMachineRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, TimeMachineRecipe> streamCodec() {
        return STREAM;
    }
}
