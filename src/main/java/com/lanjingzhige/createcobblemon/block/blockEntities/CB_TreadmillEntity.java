package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
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

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 跑步机方块实体。
 * <p>
 * 服务端：只保存宝可梦的 NBT 快照（不从电脑实体化到世界中），是机械动力发电机。
 * 客户端：根据同步的 NBT 重建一个“合成”的 PokemonEntity（不加入世界），由方块实体渲染器
 * 每帧渲染，并在 tick 中手动推进其动画年龄以播放行走动画。
 * <p>
 * 应力：驱动转速 = 宝可梦当前速度值（RPM），容量 32 SU/RPM → 宝可梦速度越快，提供的应力（SU）越高。
 */
public class CB_TreadmillEntity extends GeneratingKineticBlockEntity {

    private static final String KEY_POKEMON_NBT = "PokemonNbt";
    private static final String KEY_POKEMON_UUID = "PokemonUuid";
    private static final String KEY_PC_UUID = "PcUuid";
    private static final String KEY_OWNER_UUID = "OwnerUuid";
    /** 服务端在 setPokemon 时缓存的引用；chunk 重载或客户端上为 null，需从 NBT 懒加载 */
    private Pokemon pokemon;

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
        Pokemon pokemon = getPokemon();
        if (pokemon == null)
            return 0.0f;
        // 直接用宝可梦当前速度值作为生成转速（RPM）。
        // Create 网络中发电机提供的应力 = 容量(32 SU/RPM) × |转速|，因此宝可梦速度越快，应力越高。
        return pokemon.getStat(Stats.SPEED);
    }

    /**
     * 返回当前宝可梦；没有宝可梦、数据不可用或世界尚未加载时返回 null。
     * <p>
     * 服务端 setPokemon() 时缓存引用；chunk 重载后 read() 只恢复 NBT 快照、引用丢失，
     * 因此这里按需从 NBT 懒加载并缓存（客户端同样适用，护目镜悬浮信息等场景会调用）。
     */
    @Nullable
    private Pokemon getPokemon() {
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
        this.pokemon = pokemon;

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
        // 数据可能已变化，缓存的宝可梦引用与客户端实体均需在下次访问时重建
        this.pokemon = null;
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
        if (level != null && level.isClientSide() && clientPokemonEntity != null) {
            clientPokemonEntity.getDelegate().tick(clientPokemonEntity);
            driveWalkAnimation();
        }
    }

    /**
     * 推进合成实体的原版步态状态（{@code walkAnimation}）。
     * <p>
     * Cobblemon 的行走动画分两类：
     * <ul>
     *   <li>由 {@code q.anim_time} 驱动的骨骼动画（如 {@code q.bedrock('x', 'ground_walk')}），
     *       只要动画年龄在推进就会动，跑步机上的合成宝可梦没问题；</li>
     *   <li>函数动画（{@code q.quadruped_walk / q.biped_walk / q.bimanual_swing}），
     *       直接使用原版的 {@code limbSwing / limbSwingAmount}（见
     *       {@code LivingEntity#calculateEntityAnimation}，振幅来自实体实际移动量）。</li>
     * </ul>
     * 跑步机上的合成实体从不移动，第二类的两个值恒为 0，于是猫鼬少、魅力喵等
     * 以顺腿函数动画行走的宝可梦会僵立在跑步机上（姿势是行走姿势，但腿完全不动）。
     * 这里代替实体每 tick 递增步态相位：{@code walkAnimation} 推进后，渲染时由
     * {@code MobRenderer} 通过 {@code walkAnimation.speed(partialTicks)}（振幅）与
     * {@code walkAnimation.position(partialTicks)}（相位）插值，顺腿动画即可正常播放。
     * 步态速率与跑步机实际转速（RPM）联动：转速越高跑得越快，最低保底原地小跑。
     */
    private void driveWalkAnimation() {
        float target = Mth.clamp(Math.abs(getSpeed()) / 64.0F, 0.15F, 0.6F);
        clientPokemonEntity.walkAnimation.update(target, 1.0F);
    }
}
