package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_CreatPokemonMachine;
import com.lanjingzhige.createcobblemon.recipe.recipes.CR_Garbage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.UUID;

public class CBE_Mulch  extends CBE_CreatPokemonMachine {
    public static final int DEFAULT_RECIPE_TIME = 2000 ;

    public CBE_Mulch(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);

        inputInv = new ItemStackHandler(0);
        outputInv = new ItemStackHandler(3);
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
        ItemStack bonemeal = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cobblemon:mulch_base")));
        ItemHandlerHelper.insertItemStacked(outputInv, bonemeal, false);

        timer = DEFAULT_RECIPE_TIME;
        sendData();
        setChanged();
    }

    @Override
    protected void ensureClientPokemonEntity(PoseType pose) {
        super.ensureClientPokemonEntity(PoseType.WALK);
    }

    @Override
    public void setPokemon(ServerPlayer player, UUID uuid, String types) {
        super.setPokemon(player, uuid, "grass");

    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntity.CBE_MULCH.get(),
                (be, context) -> be.capability
        );
    }
}
