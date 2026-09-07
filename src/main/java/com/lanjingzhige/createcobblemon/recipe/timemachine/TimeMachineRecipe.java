package com.lanjingzhige.createcobblemon.recipe.timemachine;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 时光机配方：最多 6 个物品输入（槽位可空），当 6 个物品臂上的物品与输入一致时
 * 判定命中，结果为一只要被召唤的野生宝可梦（物种 + 等级，默认 70 级）。
 */
public class TimeMachineRecipe implements Recipe<TimeMachineRecipeInput> {

    private final List<Ingredient> inputs;
    private final ResourceLocation speciesId;
    private final int level;

    public TimeMachineRecipe(List<Ingredient> inputs, ResourceLocation speciesId, int level) {
        this.inputs = inputs;
        this.speciesId = speciesId;
        this.level = level;
    }

    /**
     * 匹配规则（顺序无关）：
     * 每个配方输入在 6 个物品臂中找一个未占用的槽位匹配即可，
     * 配方未使用的槽位必须为空。
     */
    @Override
    public boolean matches(TimeMachineRecipeInput inv, Level level) {
        boolean[] used = new boolean[inv.size()];
        for (Ingredient ingredient : inputs) {
            boolean found = false;
            for (int i = 0; i < inv.size(); i++) {
                if (used[i])
                    continue;
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        // 配方未使用的槽位必须为空
        for (int i = 0; i < inv.size(); i++)
            if (!used[i] && !inv.getItem(i).isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack assemble(TimeMachineRecipeInput inv, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModTimeMachineRecipe.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModTimeMachineRecipe.TYPE.get();
    }

    public List<Ingredient> getInputs() {
        return inputs;
    }

    public ResourceLocation getSpeciesId() {
        return speciesId;
    }

    public int getLevel() {
        return level;
    }

    /** 实际使用的输入槽数量（0~6） */
    public int getInputCount() {
        return inputs.size();
    }

    /** 配方是否有效：至少需要 1 个输入物品 */
    public boolean isValid() {
        return !inputs.isEmpty();
    }
}
