package com.lanjingzhige.createcobblemon.recipe.recipes;

import com.lanjingzhige.createcobblemon.recipe.ModRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class CR_CoalFluid extends StandardProcessingRecipe<SingleRecipeInput> {

    public CR_CoalFluid(ProcessingRecipeParams params) {
        super(ModRecipe.COAL_FLUID, params);
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 4;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    public boolean matches(SingleRecipeInput singleRecipeInput, Level level) {
        if (singleRecipeInput.isEmpty())
            return false;
        return ingredients.get(0).test(singleRecipeInput.getItem(0));
    }
}
