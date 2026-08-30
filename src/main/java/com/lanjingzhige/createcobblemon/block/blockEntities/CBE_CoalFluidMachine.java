package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.recipe.ModRecipe;
import com.lanjingzhige.createcobblemon.recipe.recipes.CR_CoalFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.Optional;

/**
 * 煤炭流体机：投入煤炭后按配方生成流体（默认配方产出岩浆）。
 * 流体存储功能由父类 CBE_CreatPokemonFluidMachine 提供，本类只决定容量与允许流体。
 */
public class CBE_CoalFluidMachine extends CBE_CreatPokemonFluidMachine {

    public static final int DEFAULT_RECIPE_TIME = 100;
    public static final int TANK_CAPACITY = 2000;

    public CR_CoalFluid lastRecipe;

    public CBE_CoalFluidMachine(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        inputInv = new ItemStackHandler(1);
        outputInv = new ItemStackHandler(1);
        capability = new CreatPokemonMachineInventoryHandler();
    }

    @Override
    protected int getFluidCapacity() {
        return TANK_CAPACITY;
    }

    @Override
    protected boolean isFluidAllowed(FluidStack stack) {
        return stack.getFluid() == net.minecraft.world.level.material.Fluids.LAVA;
    }

    @Override
    public int getProcessingSpeed() {
        return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
    }

    private int getProcessingTime(CR_CoalFluid recipe) {
        int duration = recipe.getProcessingDuration();
        return duration > 0 ? duration : DEFAULT_RECIPE_TIME;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide)
            return;
        if (getSpeed() == 0)
            return;

        boolean hasOutputSpace = false;
        for (int i = 0; i < outputInv.getSlots(); i++) {
            if (outputInv.getStackInSlot(i).getCount() < outputInv.getSlotLimit(i)) {
                hasOutputSpace = true;
                break;
            }
        }
        // 该机器主要产出流体，物品输出槽仅作缓冲；只要流体槽有空间即可继续
        if (!hasOutputSpace && fluidTank != null && fluidTank.getPrimaryHandler().getFluidAmount() >= fluidTank.getPrimaryHandler().getCapacity())
            return;

        if (timer > 0) {
            timer -= getProcessingSpeed();
            if (timer <= 0)
                process();
            return;
        }

        if (inputInv.getStackInSlot(0).isEmpty())
            return;

        SingleRecipeInput inventoryIn = new SingleRecipeInput(inputInv.getStackInSlot(0));
        if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
            Optional<RecipeHolder<CR_CoalFluid>> recipe = ModRecipe.COAL_FLUID.find(inventoryIn, level);
            if (recipe.isEmpty()) {
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
    }

    @Override
    void process() {
        SingleRecipeInput inventoryIn = new SingleRecipeInput(inputInv.getStackInSlot(0));
        if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
            Optional<RecipeHolder<CR_CoalFluid>> recipe = ModRecipe.COAL_FLUID.find(inventoryIn, level);
            if (recipe.isEmpty())
                return;
            lastRecipe = recipe.get().value();
        }

        // 先确认所有流体产物都能放入，避免消耗煤炭后因槽满损失产物
        for (FluidStack fluid : lastRecipe.getFluidResults()) {
            if (fillTankAmount(fluid, true) != fluid.getAmount())
                return;
        }

        ItemStack stackInSlot = inputInv.getStackInSlot(0);
        stackInSlot.shrink(1);
        inputInv.setStackInSlot(0, stackInSlot);

        for (FluidStack fluid : lastRecipe.getFluidResults())
            fillTank(fluid, false);

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
        Optional<RecipeHolder<CR_CoalFluid>> recipe = ModRecipe.COAL_FLUID.find(inventoryIn, level);
        return recipe.isPresent();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntity.CBE_COAL_FLUID.get(),
                (be, context) -> be.capability
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntity.CBE_COAL_FLUID.get(),
                (be, context) -> be.getFluidTank() == null ? null : be.getFluidTank().getCapability()
        );
    }
}
