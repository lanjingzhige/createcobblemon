package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.recipe.ModRecipe;
import com.lanjingzhige.createcobblemon.recipe.recipes.CR_Garbage;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

public class CBE_Garbage extends CBE_CreatPokemonMachine {

    /** 配方未指定 processing_time 时使用的默认加工时长 */
    public static final int DEFAULT_RECIPE_TIME = 100;

    public CR_Garbage lastRecipe;

    public CBE_Garbage(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inputInv = new ItemStackHandler(3);
        outputInv = new ItemStackHandler(9);
        capability = new CreatPokemonMachineInventoryHandler();
    }

    @Override
    public int getProcessingSpeed() {
        Pokemon pokemon = getPokemon();
        if (pokemon == null) {
            return 0;
        }
        float attack = pokemon.getStat(Stats.SPECIAL_ATTACK);
        return Mth.clamp((int) Math.abs(getSpeed() / 16f + attack / 16f), 1, 512);
    }

    private int getProcessingTime(CR_Garbage recipe) {
        int duration = recipe.getProcessingDuration();
        return duration > 0 ? duration : DEFAULT_RECIPE_TIME;
    }

    @Override
    public void tick() {
        super.tick();

        if (getSpeed() == 0)
            return;
        if (getPokemon() == null)
            return;
        for (int i = 0; i < outputInv.getSlots(); i++)
            if (outputInv.getStackInSlot(i)
                    .getCount() == outputInv.getSlotLimit(i))
                return;

        if (timer > 0) {
            timer -= getProcessingSpeed();

            if (level.isClientSide) {
                return;
            }
            if (timer <= 0)
                process();
            return;
        }

        if (inputInv.getStackInSlot(0)
                .isEmpty())
            return;

        SingleRecipeInput inventoryIn = new SingleRecipeInput(inputInv.getStackInSlot(0));
        if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
            Optional<RecipeHolder<CR_Garbage>> recipe = ModRecipe.CLEAN.find(inventoryIn, level);
            if (!recipe.isPresent()) {
                timer = DEFAULT_RECIPE_TIME;
                sendData();
            } else {
                lastRecipe = recipe.get().value();
                timer = getProcessingTime(lastRecipe);
                sendData();
            }
            return;
        }

        timer = getProcessingTime(lastRecipe);
        sendData();

        if (level != null && level.isClientSide() && clientPokemonEntity != null) {
            clientPokemonEntity.getDelegate().tick(clientPokemonEntity);
            driveWalkAnimation();
        }
    }

    @Override
    void process() {
        SingleRecipeInput inventoryIn = new SingleRecipeInput(inputInv.getStackInSlot(0));

        if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
            Optional<RecipeHolder<CR_Garbage>> recipe = ModRecipe.CLEAN.find(inventoryIn, level);
            if (recipe.isEmpty())
                return;
            lastRecipe = recipe.get().value();
        }

        ItemStack stackInSlot = inputInv.getStackInSlot(0);
        ItemStack craftingRemainingItem = stackInSlot.getCraftingRemainingItem();
        stackInSlot.shrink(1);
        inputInv.setStackInSlot(0, stackInSlot);
        lastRecipe.rollResults(level.random)
                .forEach(stack -> ItemHandlerHelper.insertItemStacked(outputInv, stack, false));
        if (!craftingRemainingItem.isEmpty()) {
            ItemHandlerHelper.insertItemStacked(outputInv, craftingRemainingItem, false);
        }
        award(AllAdvancements.MILLSTONE);

        sendData();
        setChanged();
    }

    @Override
    public boolean canProcess(ItemStack stack) {
        ItemStackHandler tester = new ItemStackHandler(1);
        tester.setStackInSlot(0, stack);
        SingleRecipeInput inventoryIn = new SingleRecipeInput(tester.getStackInSlot(0));

        if (lastRecipe != null && lastRecipe.matches(inventoryIn, level))
            return true;
        return ModRecipe.CLEAN.find(inventoryIn, level)
                .isPresent();
    }
}
