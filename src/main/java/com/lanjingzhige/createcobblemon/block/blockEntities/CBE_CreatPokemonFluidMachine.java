package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.List;

/**
 * 宝可梦流体机器的公共父类。
 * 在 CBE_CreatPokemonMachine 的基础上增加流体存储能力，流体容量与允许的流体类型由子类决定。
 */
public abstract class CBE_CreatPokemonFluidMachine extends CBE_CreatPokemonMachine {

    protected SmartFluidTankBehaviour fluidTank;

    public CBE_CreatPokemonFluidMachine(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        fluidTank = SmartFluidTankBehaviour.single(this, getFluidCapacity())
                .allowInsertion()
                .allowExtraction();
        behaviours.add(fluidTank);
    }

    /** 子类决定该机器流体槽的容量（mB）。 */
    protected abstract int getFluidCapacity();

    /** 子类决定允许存储的流体类型；默认不限制。 */
    protected boolean isFluidAllowed(FluidStack stack) {
        return true;
    }

    public SmartFluidTankBehaviour getFluidTank() {
        return fluidTank;
    }

    protected SmartFluidTank getPrimaryTank() {
        return fluidTank == null ? null : fluidTank.getPrimaryHandler();
    }

    protected boolean fillTank(FluidStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty() || !isFluidAllowed(stack))
            return false;
        SmartFluidTank tank = getPrimaryTank();
        if (tank == null)
            return false;
        int filled = tank.fill(stack, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
        if (!simulate && filled > 0) {
            fluidTank.sendDataImmediately();
            setChanged();
        }
        return filled > 0;
    }

    protected int fillTankAmount(FluidStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty() || !isFluidAllowed(stack))
            return 0;
        SmartFluidTank tank = getPrimaryTank();
        if (tank == null)
            return 0;
        int filled = tank.fill(stack, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
        if (!simulate && filled > 0) {
            fluidTank.sendDataImmediately();
            setChanged();
        }
        return filled;
    }
}
