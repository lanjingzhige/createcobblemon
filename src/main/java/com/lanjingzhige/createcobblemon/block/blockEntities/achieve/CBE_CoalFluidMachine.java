package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_CreatPokemonFluidMachine;
import com.lanjingzhige.createcobblemon.block.blocks.CB_CoalFluidMachine;
import com.lanjingzhige.createcobblemon.recipe.ModRecipe;
import com.lanjingzhige.createcobblemon.recipe.recipes.CR_Lava;
import com.lanjingzhige.createcobblemon.recipe.recipes.CR_Water;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 煤炭流体机：投入煤炭后按配方生成流体（默认配方产出岩浆）。
 * 流体存储功能由父类 CBE_CreatPokemonFluidMachine 提供，本类只决定容量与允许流体。
 */
public class CBE_CoalFluidMachine extends CBE_CreatPokemonFluidMachine {

    public static final int DEFAULT_RECIPE_TIME = 100;
    public static final int TANK_CAPACITY = 2000;
    public static boolean lava = false;

    public CR_Lava lastRecipelava;
    public CR_Water lastRecipewater;

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
    public int getProcessingSpeed() {
        Pokemon pokemon = getPokemon();
        if (pokemon == null) {
            return 0;
        }
        float attack = pokemon.getStat(Stats.SPECIAL_ATTACK);
        return Mth.clamp((int) Math.abs(getSpeed() / 16f + attack / 16f), 1, 512);
    }

    private int getProcessingTimelava(CR_Lava recipe) {
        int duration = recipe.getProcessingDuration();
        return duration > 0 ? duration : DEFAULT_RECIPE_TIME;
    }
    private int getProcessingTimewater(CR_Water recipe) {
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
        if (lava){
            if (lastRecipelava == null || !lastRecipelava.matches(inventoryIn, level)) {

                Optional<RecipeHolder<CR_Lava>> recipe = ModRecipe.LAVA.find(inventoryIn, level);

                if (recipe.isEmpty()) {
                    timer = DEFAULT_RECIPE_TIME;
                    sendData();
                } else {
                    lastRecipelava = recipe.get().value();
                    timer = getProcessingTimelava(lastRecipelava);
                    sendData();
                }
                return;
            }

            timer = getProcessingTimelava(lastRecipelava);
            sendData();
        }else {
            if (lastRecipewater == null || !lastRecipewater.matches(inventoryIn, level)) {

                Optional<RecipeHolder<CR_Water>> recipe = ModRecipe.WATER.find(inventoryIn, level);

                if (recipe.isEmpty()) {
                    timer = DEFAULT_RECIPE_TIME;
                    sendData();
                } else {
                    lastRecipewater = recipe.get().value();
                    timer = getProcessingTimewater(lastRecipewater);
                    sendData();
                }
                return;
            }
            timer = getProcessingTimewater(lastRecipewater);
            sendData();
        }



    }


    void process() {
        SingleRecipeInput inventoryIn = new SingleRecipeInput(inputInv.getStackInSlot(0));
        if (lava){
            if (lastRecipelava == null || !lastRecipelava.matches(inventoryIn, level)) {
                Optional<RecipeHolder<CR_Lava>> recipe= ModRecipe.LAVA.find(inventoryIn, level);

                if (recipe.isEmpty())
                    return;
                lastRecipelava = recipe.get().value();
            }

            // 先确认所有流体产物都能放入，避免消耗煤炭后因槽满损失产物
            for (FluidStack fluid : lastRecipelava.getFluidResults()) {
                if (fillTankAmount(fluid, true) != fluid.getAmount())
                    return;
            }

            ItemStack stackInSlot = inputInv.getStackInSlot(0);
            stackInSlot.shrink(1);
            inputInv.setStackInSlot(0, stackInSlot);

            for (FluidStack fluid : lastRecipelava.getFluidResults())
                fillTank(fluid, false);
        }else {
            if (lastRecipewater == null || !lastRecipewater.matches(inventoryIn, level)) {
                Optional<RecipeHolder<CR_Water>> recipe= ModRecipe.WATER.find(inventoryIn, level);

                if (recipe.isEmpty())
                    return;
                lastRecipewater = recipe.get().value();
            }

            // 先确认所有流体产物都能放入，避免消耗煤炭后因槽满损失产物
            for (FluidStack fluid : lastRecipewater.getFluidResults()) {
                if (fillTankAmount(fluid, true) != fluid.getAmount())
                    return;
            }

            ItemStack stackInSlot = inputInv.getStackInSlot(0);
            stackInSlot.shrink(1);
            inputInv.setStackInSlot(0, stackInSlot);

            for (FluidStack fluid : lastRecipewater.getFluidResults())
                fillTank(fluid, false);
        }


        sendData();
        setChanged();
    }


    @Override
    public boolean canProcess(ItemStack stack) {
        ItemStackHandler tester = new ItemStackHandler(1);
        tester.setStackInSlot(0, stack);
        SingleRecipeInput inventoryIn = new SingleRecipeInput(tester.getStackInSlot(0));

        if (lava){
            if (lastRecipelava != null && lastRecipelava.matches(inventoryIn, level))
                return true;
            Optional<RecipeHolder<CR_Lava>> recipe= ModRecipe.LAVA.find(inventoryIn, level);
            return recipe.isPresent();
        }else {
            if (lastRecipewater != null && lastRecipewater.matches(inventoryIn, level))
                return true;
            Optional<RecipeHolder<CR_Water>> recipe= ModRecipe.WATER.find(inventoryIn, level);
            return recipe.isPresent();
        }



    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void setPokemon(ServerPlayer player, UUID uuid) {
        if (level == null || level.isClientSide)
            return;
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        Pokemon pokemon = pc.get(uuid);
        if (pokemon == null)
            return;

        if (!pokemon.getPrimaryType().showdownId().equals("fire")
                && !pokemon.getPrimaryType().showdownId().equals("water")
                && !Objects.requireNonNull(pokemon.getSecondaryType()).showdownId().equals("fire")
                && !Objects.requireNonNull(pokemon.getSecondaryType()).showdownId().equals("water") ){
            return;
        }
        if (hasPokemon())
            releaseToStore();

        pc.remove(pokemon);
        this.pokemonNbt = pokemon.saveToNBT(level.registryAccess(), new CompoundTag());
        this.pokemonUuid = uuid;
        this.pcUuid = pc.getUuid();
        this.ownerUuid = player.getUUID();
        this.clientPokemonEntity = null;
        this.pokemon = pokemon;

        notifyChange();

        for (ElementalType type : pokemon.getTypes()) {
            if (type.showdownId().equals("fire")){
                level.setBlock(worldPosition,
                        getBlockState().setValue(CB_CoalFluidMachine.LAVA, true),
                        3);
                lava = true;
            }
            if (type.showdownId().equals("water")){
                level.setBlock(worldPosition,
                        getBlockState().setValue(CB_CoalFluidMachine.LAVA, false),
                        3);
                lava = false;
            }
        }

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
