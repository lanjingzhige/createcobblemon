package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Treadmill;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.UUID;

public class CBE_CreatPokemonGenerator extends GeneratingKineticBlockEntity {
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

    public CBE_CreatPokemonGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasPokemon() {
        return pokemonNbt != null;
    }


    /** 懒加载返回当前宝可梦（服务端缓存引用；客户端按需从 NBT 重建并缓存），没有时为 null */
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

    @Override
    public void initialize() {
        super.initialize();
        updateGeneratedRotation();
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

        updateGeneratedRotation();
        notifyChange();
    }

    public void releasePokemon() {
        if (level == null || level.isClientSide)
            return;
        if (!hasPokemon())
            return;
        releaseToStore();
        clearPokemonData();
        updateGeneratedRotation();
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

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // 宝可梦 NBT 也要同步到客户端（渲染需要），所以不区分 clientPacket 都写
        if (pokemonNbt != null)
            tag.put(KEY_POKEMON_NBT, pokemonNbt);
        if (pokemonUuid != null)
            tag.putUUID(KEY_POKEMON_UUID, pokemonUuid);
        if (pcUuid != null)
            tag.putUUID(KEY_PC_UUID, pcUuid);
        if (ownerUuid != null)
            tag.putUUID(KEY_OWNER_UUID, ownerUuid);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.pokemonNbt = tag.contains(KEY_POKEMON_NBT) ? tag.getCompound(KEY_POKEMON_NBT) : null;
        this.pokemonUuid = tag.hasUUID(KEY_POKEMON_UUID) ? tag.getUUID(KEY_POKEMON_UUID) : null;
        this.pcUuid = tag.hasUUID(KEY_PC_UUID) ? tag.getUUID(KEY_PC_UUID) : null;
        this.ownerUuid = tag.hasUUID(KEY_OWNER_UUID) ? tag.getUUID(KEY_OWNER_UUID) : null;
        // 数据可能已变化，缓存的宝可梦引用与客户端实体均需在下次访问时重建
        this.pokemon = null;
        this.clientPokemonEntity = null;
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

    @Override
    public void tick() {
        super.tick();
        // 客户端：手动推进合成实体的动画年龄（delegate.tick -> incrementAge）
        if (level != null && level.isClientSide() && clientPokemonEntity != null) {
            clientPokemonEntity.getDelegate().tick(clientPokemonEntity);
            driveWalkAnimation();
        }
    }

    private void driveWalkAnimation() {
        float target = Mth.clamp(Math.abs(getSpeed()) / 64.0F, 0.15F, 0.6F);
        clientPokemonEntity.walkAnimation.update(target, 1.0F);
    }

}
