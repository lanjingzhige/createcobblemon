package com.lanjingzhige.createcobblemon.recipe.recipes;

import com.lanjingzhige.createcobblemon.recipe.ModRecipe;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class CR_Garbage extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {
    public CR_Garbage(ProcessingRecipeParams params) {
        super(ModRecipe.CLEAN, params);
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 3;
    }

    // 本机使用配方的 processing_time 作为加工时长，必须允许配方指定时长，
    // 否则带时长的配方会在数据包加载时校验失败而被直接丢弃（无法处理配方）
    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public Component getDescriptionForAssembly() {
        return null;
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {

    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {

    }

    @Override
    public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
        return null;
    }

    @Override
    public boolean matches(SingleRecipeInput singleRecipeInput, Level level) {
        if (singleRecipeInput.isEmpty())
            return false;
        return ingredients.get(0)
                .test(singleRecipeInput.getItem(0));
    }
}
