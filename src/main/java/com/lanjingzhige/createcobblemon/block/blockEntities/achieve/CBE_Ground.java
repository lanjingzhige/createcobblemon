package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.CI_ControlContraption;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_CreatPokemonMachine;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Ground;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Ice;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;

public class CBE_Ground extends CBE_CreatPokemonMachine implements CI_ControlContraption {

    public static final int DEFAULT_RECIPE_TIME = 600 ;
    public ScrollOptionBehaviour<MovementGroundMode> movementMode;

    public CBE_Ground(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);

        inputInv = new ItemStackHandler(0);
        outputInv = new ItemStackHandler(3);
        capability = new CreatPokemonMachineInventoryHandler();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        movementMode = new ScrollOptionBehaviour<>(MovementGroundMode.class, Component.translatable("contraptions.movement_mode"),
                this, new CBE_Ground.NoteblockValueBoxTransform());
        behaviours.add(movementMode);
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


    @Override
    public void tick() {
        super.tick();

        if (getSpeed() == 0)
            return;
        if (getPokemon() == null)
            return;
        boolean hasOutputSpace = false;
        for (int i = 0; i < outputInv.getSlots(); i++) {
            if (outputInv.getStackInSlot(i).getCount() < outputInv.getSlotLimit(i)) {
                hasOutputSpace = true;
                break;
            }
        }
        if (!hasOutputSpace)
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

        timer = DEFAULT_RECIPE_TIME;
        sendData();
    }

    void process(){
        if (getMovementMode() == MovementGroundMode.MOVE_CLAY){
            ItemStack bonemeal = new ItemStack(Items.CLAY,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
        }
        if (getMovementMode() == MovementGroundMode.MOVE_DIRT){
            ItemStack bonemeal = new ItemStack(Items.DIRT,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
        }
        if (getMovementMode() == MovementGroundMode.MOVE_SAND){
            ItemStack bonemeal = new ItemStack(Items.SAND,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
        }


        timer = DEFAULT_RECIPE_TIME;
        sendData();
        setChanged();
    }


    protected MovementGroundMode getMovementMode() {
        return movementMode.get();
    }

    private class NoteblockValueBoxTransform extends ValueBoxTransform.Sided {
        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = state.getValue(CB_Ground.FACING);
            return direction == facing;
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8,12,16.05f);
        }
    }

    @Override
    protected void ensureClientPokemonEntity(PoseType pose) {
        super.ensureClientPokemonEntity(PoseType.WALK);
    }

    @Override
    public void setPokemon(ServerPlayer player, UUID uuid, String types) {
        super.setPokemon(player, uuid, "ground");

    }


    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntity.CBE_GROUND.get(),
                (be, context) -> be.capability
        );
    }
}