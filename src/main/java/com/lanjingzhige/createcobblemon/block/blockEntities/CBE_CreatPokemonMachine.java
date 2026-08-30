package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.List;
import java.util.Optional;

public class CBE_CreatPokemonMachine extends CBE_CreatPokemonBase {
    public ItemStackHandler inputInv;
    public ItemStackHandler outputInv;
    public IItemHandler capability;
    public int timer;
    private MillingRecipe lastRecipe;

    public CBE_CreatPokemonMachine(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this));
        super.addBehaviours(behaviours);
        registerAwardables(behaviours, AllAdvancements.MILLSTONE);
    }

    public int getProcessingSpeed() {
        return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
    }

     void process() {
        RecipeWrapper inventoryIn = new RecipeWrapper(inputInv);

        if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
            Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, level);
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

        sendData();
        setChanged();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inputInv);
        ItemHelper.dropContents(level, worldPosition, outputInv);
    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt("Timer", timer);
        compound.put("InputInventory", inputInv.serializeNBT(registries));
        compound.put("OutputInventory", outputInv.serializeNBT(registries));
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        timer = compound.getInt("Timer");
        inputInv.deserializeNBT(registries, compound.getCompound("InputInventory"));
        outputInv.deserializeNBT(registries, compound.getCompound("OutputInventory"));
        super.read(compound, registries, clientPacket);
    }

    public boolean canProcess(ItemStack stack) {
        ItemStackHandler tester = new ItemStackHandler(1);
        tester.setStackInSlot(0, stack);
        RecipeWrapper inventoryIn = new RecipeWrapper(tester);

        if (lastRecipe != null && lastRecipe.matches(inventoryIn, level))
            return true;
        return AllRecipeTypes.MILLING.find(inventoryIn, level)
                .isPresent();
    }

    public class CreatPokemonMachineInventoryHandler extends CombinedInvWrapper {

        public CreatPokemonMachineInventoryHandler() {
            super(inputInv, outputInv);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (outputInv == getHandlerFromIndex(getIndexForSlot(slot)))
                return false;
            return canProcess(stack) && super.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (outputInv == getHandlerFromIndex(getIndexForSlot(slot)))
                return stack;
            if (!isItemValid(slot, stack))
                return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (inputInv == getHandlerFromIndex(getIndexForSlot(slot)))
                return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }
    }
}
