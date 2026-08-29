package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Treadmill;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.UUID;

public class CBE_CreatPokemonMachine extends KineticBlockEntity {
    public ItemStackHandler inputInv;
    public ItemStackHandler outputInv;
    public IItemHandler capability;
    public int timer;
    private MillingRecipe lastRecipe;

    private static final String KEY_POKEMON_NBT = "PokemonNbt";
    private static final String KEY_POKEMON_UUID = "PokemonUuid";
    private static final String KEY_PC_UUID = "PcUuid";
    private static final String KEY_OWNER_UUID = "OwnerUuid";
    protected Pokemon pokemon;
    protected CompoundTag pokemonNbt;
    protected UUID pokemonUuid;
    protected UUID pcUuid;
    protected UUID ownerUuid;

    protected PokemonEntity clientPokemonEntity;

    public CBE_CreatPokemonMachine(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public boolean hasPokemon() {
        return pokemonNbt != null;
    }

    protected Pokemon getPokemon() {
        if (pokemon != null)
            return pokemon;
        if (pokemonNbt == null || level == null)
            return null;
        try {
            pokemon = new Pokemon().loadFromNBT(level.registryAccess(), pokemonNbt);
        } catch (Exception e) {
            // 数据不合法时视为没有宝可梦，不影响方块功能
            pokemon = null;
        }
        return pokemon;
    }

    public void setPokemon(ServerPlayer player, UUID uuid) {
        if (level == null || level.isClientSide)
            return;
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        Pokemon pokemon = pc.get(uuid);
        if (pokemon == null)
            return;

        // 替换：先把旧的还回它的电脑
        if (hasPokemon())
            releaseToStore();

        pc.remove(pokemon);
        this.pokemonNbt = pokemon.saveToNBT(level.registryAccess(), new CompoundTag());
        this.pokemonUuid = uuid;
        this.pcUuid = pc.getUuid();
        this.ownerUuid = player.getUUID();
        this.clientPokemonEntity = null;
        this.pokemon = pokemon;
        for (ElementalType type : pokemon.getTypes()) {
            if (type.showdownId().equals("flying")){
                level.setBlock(worldPosition,
                        getBlockState().setValue(CB_Treadmill.FLY, true),
                        3);
            }
        }

        notifyChange();
    }

    public void releasePokemon() {
        if (level == null || level.isClientSide)
            return;
        if (!hasPokemon())
            return;
        releaseToStore();
        clearPokemonData();
        notifyChange();
    }



    private void releaseToStore() {
        if (pokemonNbt == null || pcUuid == null || level == null)
            return;
        RegistryAccess registryAccess = level.registryAccess();
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(pcUuid, registryAccess);
        // 只有电脑里还没有这只宝可梦时才归还，避免重复
        if (pc != null && pc.get(pokemonUuid) == null) {
            Pokemon restored = new Pokemon().loadFromNBT(registryAccess, pokemonNbt);
            pc.add(restored);
        }
    }

    private void clearPokemonData() {
        this.pokemon = null;
        this.pokemonNbt = null;
        this.pokemonUuid = null;
        this.pcUuid = null;
        this.ownerUuid = null;
        this.clientPokemonEntity = null;
    }

    private void notifyChange() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

    public PokemonEntity getClientPokemonEntity() {
        ensureClientPokemonEntity(null);
        return clientPokemonEntity;
    }

    protected void ensureClientPokemonEntity(PoseType pose) {
        if (clientPokemonEntity != null || level == null || !level.isClientSide())
            return;
        if (pokemonNbt == null)
            return;
        try {
            Pokemon pokemon = getPokemon();
            if (pokemon == null)
                return;
            clientPokemonEntity = new PokemonEntity(level, pokemon, CobblemonEntities.POKEMON);
            clientPokemonEntity.setEnablePoseTypeRecalculation(false);
            clientPokemonEntity.setNoAi(true);
            if (pose == null){

            }else {
                clientPokemonEntity.getEntityData().set(PokemonEntity.getPOSE_TYPE(), pose);
            }

            clientPokemonEntity.setInvulnerable(true);
            clientPokemonEntity.hideNameRendering();
            clientPokemonEntity.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
        } catch (Exception e) {
            // 数据不合法时放弃渲染，不影响方块功能
            clientPokemonEntity = null;
        }
    }

/*
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AllBlockEntityTypes.MILLSTONE.get(),
                (be, context) -> be.capability
        );
    }
*/


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new DirectBeltInputBehaviour(this));
        super.addBehaviours(behaviours);
        registerAwardables(behaviours, AllAdvancements.MILLSTONE);
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

        RecipeWrapper inventoryIn = new RecipeWrapper(inputInv);
        if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
            Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, level);
            if (!recipe.isPresent()) {
                timer = 100;
                sendData();
            } else {
                lastRecipe = recipe.get().value();
                timer = lastRecipe.getProcessingDuration();
                sendData();
            }
            return;
        }

        timer = lastRecipe.getProcessingDuration();
        sendData();

        if (level != null && level.isClientSide() && clientPokemonEntity != null) {
            clientPokemonEntity.getDelegate().tick(clientPokemonEntity);
            driveWalkAnimation();
        }
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
        award(AllAdvancements.MILLSTONE);

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

        if (pokemonNbt != null)
            compound.put(KEY_POKEMON_NBT, pokemonNbt);
        if (pokemonUuid != null)
            compound.putUUID(KEY_POKEMON_UUID, pokemonUuid);
        if (pcUuid != null)
            compound.putUUID(KEY_PC_UUID, pcUuid);
        if (ownerUuid != null)
            compound.putUUID(KEY_OWNER_UUID, ownerUuid);
    }
    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        timer = compound.getInt("Timer");
        inputInv.deserializeNBT(registries, compound.getCompound("InputInventory"));
        outputInv.deserializeNBT(registries, compound.getCompound("OutputInventory"));
        super.read(compound, registries, clientPacket);

        this.pokemonNbt = compound.contains(KEY_POKEMON_NBT) ? compound.getCompound(KEY_POKEMON_NBT) : null;
        this.pokemonUuid = compound.hasUUID(KEY_POKEMON_UUID) ? compound.getUUID(KEY_POKEMON_UUID) : null;
        this.pcUuid = compound.hasUUID(KEY_PC_UUID) ? compound.getUUID(KEY_PC_UUID) : null;
        this.ownerUuid = compound.hasUUID(KEY_OWNER_UUID) ? compound.getUUID(KEY_OWNER_UUID) : null;
        // 数据可能已变化，缓存的宝可梦引用与客户端实体均需在下次访问时重建
        this.pokemon = null;
        this.clientPokemonEntity = null;
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

    void driveWalkAnimation() {
        float target = Mth.clamp(Math.abs(getSpeed()) / 64.0F, 0.15F, 0.6F);
        clientPokemonEntity.walkAnimation.update(target, 1.0F);
    }
}
