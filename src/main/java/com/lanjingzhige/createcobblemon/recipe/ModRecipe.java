package com.lanjingzhige.createcobblemon.recipe;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.recipe.recipes.CR_CoalFluid;
import com.lanjingzhige.createcobblemon.recipe.recipes.CR_Garbage;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public enum ModRecipe implements IRecipeTypeInfo {

    CLEAN(CR_Garbage::new),

    COAL_FLUID(CR_CoalFluid::new);

    public final ResourceLocation id;
    public final Supplier<RecipeSerializer<?>> serializerSupplier;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
    @Nullable
    private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;
    private final Supplier<RecipeType<?>> type;

    // 3. Constructor for Standard Processing Recipes (Matches AllRecipeTypes line 120)
    ModRecipe(StandardProcessingRecipe.Factory<?> processingFactory) {
        this(() -> new StandardProcessingRecipe.Serializer<>(processingFactory));
    }

    // 4. Main Constructor (Matches AllRecipeTypes line 109)
    ModRecipe(Supplier<RecipeSerializer<?>> serializerSupplier) {
        String name = name().toLowerCase(Locale.ROOT);
        this.id = CreateCobblemon.modLoc(name);
        this.serializerSupplier = serializerSupplier;

        this.serializerObject = Registers.SERIALIZER_REGISTER.register(name, serializerSupplier);

        this.typeObject = Registers.TYPE_REGISTER.register(name, () -> RecipeType.simple(id));
        this.type = typeObject;
    }

    // 5. Standard Registration Method
    public static void register(IEventBus modEventBus) {
        Registers.SERIALIZER_REGISTER.register(modEventBus);
        Registers.TYPE_REGISTER.register(modEventBus);
    }

    // 6. IRecipeTypeInfo Implementation
    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public <T extends RecipeSerializer<?>> T getSerializer() {
        return (T) serializerObject.get();
    }

    @Override
    public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
        return (RecipeType<R>) type.get();
    }

    // Helper to find recipes in world
    public <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(I inv, Level world) {
        return world.getRecipeManager().getRecipeFor(getType(), inv, world);
    }

    // 7. Nested Registers Class (Matches AllRecipeTypes line 186)
    private static class Registers {
        private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER =
                DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CreateCobblemon.MODID);
        private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER =
                DeferredRegister.create(Registries.RECIPE_TYPE, CreateCobblemon.MODID);
    }
}