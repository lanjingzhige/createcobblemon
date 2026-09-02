package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.CI_ControlContraption;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_CreatPokemonMachine;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Ground;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Rock;
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

public class CBE_Rock extends CBE_CreatPokemonMachine implements CI_ControlContraption {

    public static final int SHORT_TIME = 400 ;
    public static final int LONG_TIME = 600 ;
    public ScrollOptionBehaviour<MovementRockMode> movementMode;

    public CBE_Rock(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);

        inputInv = new ItemStackHandler(0);
        outputInv = new ItemStackHandler(3);
        capability = new CreatPokemonMachineInventoryHandler();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        movementMode = new ScrollOptionBehaviour<>(MovementRockMode.class, Component.translatable("contraptions.movement_mode"),
                this, new CBE_Rock.NoteblockValueBoxTransform());
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

        timer = SHORT_TIME;
        sendData();
    }

    void process(){
        if (getMovementMode() == MovementRockMode.MOVE_COBBLESTONE){
            ItemStack bonemeal = new ItemStack(Items.COBBLESTONE,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
            timer = SHORT_TIME;
        }
        if (getMovementMode() == MovementRockMode.MOVE_SMOOTH_STONE){
            ItemStack bonemeal = new ItemStack(Items.SMOOTH_STONE,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
            timer = LONG_TIME;
        }
        if (getMovementMode() == MovementRockMode.MOVE_COBBLED_DEEPSLATE){
            ItemStack bonemeal = new ItemStack(Items.COBBLED_DEEPSLATE,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
            timer = SHORT_TIME;
        }
        if (getMovementMode() == MovementRockMode.MOVE_POLISHED_DEEPSLATE){
            ItemStack bonemeal = new ItemStack(Items.POLISHED_DEEPSLATE,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
            timer = LONG_TIME;
        }
        if (getMovementMode() == MovementRockMode.MOVE_ANDESITE){
            ItemStack bonemeal = new ItemStack(Items.ANDESITE,10);
            ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);
            timer = SHORT_TIME;
        }


        sendData();
        setChanged();
    }


    protected MovementRockMode getMovementMode() {
        return movementMode.get();
    }

    private class NoteblockValueBoxTransform extends ValueBoxTransform.Sided {
        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            Direction facing = state.getValue(CB_Rock.FACING);
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
        super.setPokemon(player, uuid, "rock");

    }


    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntity.CBE_ROCK.get(),
                (be, context) -> be.capability
        );
    }
}