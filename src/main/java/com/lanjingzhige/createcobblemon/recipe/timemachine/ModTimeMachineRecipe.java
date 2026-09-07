package com.lanjingzhige.createcobblemon.recipe.timemachine;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * 时光机配方的注册表。配方 JSON 放在 data/createcobblemon/recipe/time_machine/ 目录。
 */
public class ModTimeMachineRecipe {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CreateCobblemon.MODID);
    public static final DeferredRegister<RecipeType<?>> TYPE_REGISTER =
            DeferredRegister.create(Registries.RECIPE_TYPE, CreateCobblemon.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, TimeMachineRecipeSerializer> SERIALIZER =
            SERIALIZER_REGISTER.register("time_machine", TimeMachineRecipeSerializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<TimeMachineRecipe>> TYPE =
            TYPE_REGISTER.register("time_machine", () -> RecipeType.simple(CreateCobblemon.modLoc("time_machine")));

    public static void register(IEventBus modEventBus) {
        SERIALIZER_REGISTER.register(modEventBus);
        TYPE_REGISTER.register(modEventBus);
    }

    /** 在世界配方管理器里查找与 6 个输入匹配的时光机配方 */
    public static Optional<RecipeHolder<TimeMachineRecipe>> find(TimeMachineRecipeInput inv, Level level) {
        return level.getRecipeManager().getRecipeFor(TYPE.get(), inv, level);
    }
}
