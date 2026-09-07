package com.lanjingzhige.createcobblemon.recipe.timemachine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * 时光机配方输入：固定 6 个物品槽（对应 6 个物品臂），槽位可以为空。
 */
public class TimeMachineRecipeInput implements RecipeInput {

    private final ItemStack[] stacks;

    public TimeMachineRecipeInput(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    @Override
    public ItemStack getItem(int index) {
        return stacks[index];
    }

    @Override
    public int size() {
        return stacks.length;
    }
}
