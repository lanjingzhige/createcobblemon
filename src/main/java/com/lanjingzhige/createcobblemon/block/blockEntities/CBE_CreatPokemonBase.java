package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
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

/**
 * 宝可梦相关方块实体的公共父类。
 * 将 CBE_CreatPokemonGenerator 与 CBE_CreatPokemonMachine 中重复的宝可梦存取、
 * 客户端实体渲染和 NBT 同步逻辑集中到这里，后续机械动力宝可梦机器可直接继承本类。
 */
public abstract class CBE_CreatPokemonBase extends GeneratingKineticBlockEntity {
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

    public CBE_CreatPokemonBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasPokemon() {
        return pokemonNbt != null;
    }

    /** 返回当前宝可梦（服务端缓存引用；客户端按需从 NBT 重建并缓存），没有时返回 null */
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
        setPokemon(player, uuid, null);
    }

    /**
     * 将玩家 PC 中的宝可梦放入方块。
     * types 为 null 时不限制属性；否则必须包含指定属性。
     */
    public void setPokemon(ServerPlayer player, UUID uuid, String types) {
        if (level == null || level.isClientSide)
            return;
        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(player);
        Pokemon pokemon = pc.get(uuid);
        if (pokemon == null)
            return;

        if (types != null) {
            boolean matches = pokemon.getPrimaryType().showdownId().equals(types);
            com.cobblemon.mod.common.api.types.ElementalType secondary = pokemon.getSecondaryType();
            if (!matches && secondary != null)
                matches = secondary.showdownId().equals(types);
            if (!matches)
                return;
        }

        // 替换：先把旧的归还电脑
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

    protected void releaseToStore() {
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

    protected void clearPokemonData() {
        this.pokemon = null;
        this.pokemonNbt = null;
        this.pokemonUuid = null;
        this.pcUuid = null;
        this.ownerUuid = null;
        this.clientPokemonEntity = null;
    }

    protected void notifyChange() {
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
            if (pose != null)
                clientPokemonEntity.getEntityData().set(PokemonEntity.getPOSE_TYPE(), pose);

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

    protected void driveWalkAnimation() {
        if (clientPokemonEntity == null)
            return;
        float target = Mth.clamp(Math.abs(getSpeed()) / 64.0F, 0.15F, 0.6F);
        clientPokemonEntity.walkAnimation.update(target, 1.0F);
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
        CompoundTag oldPokemonNbt = this.pokemonNbt;
        UUID oldPokemonUuid = this.pokemonUuid;
        UUID oldPcUuid = this.pcUuid;
        UUID oldOwnerUuid = this.ownerUuid;
        PokemonEntity oldClientEntity = this.clientPokemonEntity;

        super.read(tag, registries, clientPacket);
        this.pokemonNbt = tag.contains(KEY_POKEMON_NBT) ? tag.getCompound(KEY_POKEMON_NBT) : null;
        this.pokemonUuid = tag.hasUUID(KEY_POKEMON_UUID) ? tag.getUUID(KEY_POKEMON_UUID) : null;
        this.pcUuid = tag.hasUUID(KEY_PC_UUID) ? tag.getUUID(KEY_PC_UUID) : null;
        this.ownerUuid = tag.hasUUID(KEY_OWNER_UUID) ? tag.getUUID(KEY_OWNER_UUID) : null;

        boolean pokemonDataChanged = !Objects.equals(oldPokemonNbt, this.pokemonNbt)
                || !Objects.equals(oldPokemonUuid, this.pokemonUuid)
                || !Objects.equals(oldPcUuid, this.pcUuid)
                || !Objects.equals(oldOwnerUuid, this.ownerUuid);

        // 服务端或宝可梦数据变化时重建缓存；客户端仅同步库存/流体/进度时保留现有实体，避免动画卡顿
        this.pokemon = null;
        this.clientPokemonEntity = clientPacket && !pokemonDataChanged ? oldClientEntity : null;
    }
}
