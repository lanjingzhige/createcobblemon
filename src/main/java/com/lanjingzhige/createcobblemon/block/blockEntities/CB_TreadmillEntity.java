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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 跑步机方块实体。
 * <p>
 * 服务端：只保存宝可梦的 NBT 快照（不从电脑实体化到世界中），是机械动力发电机。
 * 客户端：根据同步的 NBT 重建一个“合成”的 PokemonEntity（不加入世界），由方块实体渲染器
 * 每帧渲染，并在 tick 中手动推进其动画年龄以播放行走动画。
 * <p>
 * 应力：容量 32 SU/RPM × 转速 8 RPM = 256 SU。
 */
public class CB_TreadmillEntity extends GeneratingKineticBlockEntity {

    private static final String KEY_POKEMON_NBT = "PokemonNbt";
    private static final String KEY_POKEMON_UUID = "PokemonUuid";
    private static final String KEY_PC_UUID = "PcUuid";
    private static final String KEY_OWNER_UUID = "OwnerUuid";

    /** 有宝可梦跑步时的发电机转速（RPM）。容量 32 × 转速 8 = 256 SU */
    public static final float GENERATED_SPEED = 8.0f;

    @Nullable
    private CompoundTag pokemonNbt;
    @Nullable
    private UUID pokemonUuid;
    @Nullable
    private UUID pcUuid;
    @Nullable
    private UUID ownerUuid;

    /** 客户端专用的合成实体（瞬态，不参与存档，也从不加入世界） */
    @Nullable
    private PokemonEntity clientPokemonEntity;

    public CB_TreadmillEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasPokemon() {
        return pokemonNbt != null;
    }

    @Override
    public float getGeneratedSpeed() {
        return hasPokemon() ?
                GENERATED_SPEED : 0.0f;
    }

    /**
     * 首次加载（放置或 chunk 重载）时应用一次生成转速。
     * 此时宝可梦 NBT 已通过 read() 载入，保证服务端 speed 字段与是否有宝可梦一致。
     */
    @Override
    public void initialize() {
        super.initialize();
        updateGeneratedRotation();
    }

    // ---- 服务端逻辑 ----------------------------------------------------------

    /**
     * 把玩家电脑中的宝可梦放入跑步机。若已有宝可梦则先归还，再替换。
     */
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

        updateGeneratedRotation();
        notifyChange();
    }

    /**
     * 释放当前宝可梦，归还其电脑。
     */
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

    // ---- 存档 / 同步 ---------------------------------------------------------

    // 注意：Create 6 的 SmartBlockEntity 把 saveAdditional/loadAdditional 设为 final，
    // 持久化和客户端同步都统一走 write/read(tag, registries, clientPacket) 这两个钩子，
    // 因此这里必须覆盖 write/read 而不是 saveAdditional/loadAdditional。

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
        // 数据可能已变化，客户端实体需要在下次访问时重建
        this.clientPokemonEntity = null;
    }

    // ---- 客户端渲染实体 ------------------------------------------------------

    /**
     * 返回供渲染器使用的客户端合成实体；没有宝可梦时为 null。
     */
    @Nullable
    public PokemonEntity getClientPokemonEntity() {
        ensureClientPokemonEntity();
        return clientPokemonEntity;
    }

    private void ensureClientPokemonEntity() {
        if (clientPokemonEntity != null || level == null || !level.isClientSide())
            return;
        if (pokemonNbt == null)
            return;
        try {
            Pokemon pokemon = new Pokemon().loadFromNBT(level.registryAccess(), pokemonNbt);
            clientPokemonEntity = new PokemonEntity(level, pokemon, CobblemonEntities.POKEMON);
            clientPokemonEntity.setEnablePoseTypeRecalculation(false);
            clientPokemonEntity.setNoAi(true);
            clientPokemonEntity.getEntityData().set(PokemonEntity.getPOSE_TYPE(), PoseType.WALK);
            clientPokemonEntity.setInvulnerable(true);
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
        if (level != null && level.isClientSide() && clientPokemonEntity != null)
            clientPokemonEntity.getDelegate().tick(clientPokemonEntity);
    }
}
