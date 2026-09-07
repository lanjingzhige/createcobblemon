package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.CI_ControlContraption;
import com.lanjingzhige.createcobblemon.block.behaviour.EvStatBehaviour;
import com.lanjingzhige.createcobblemon.block.behaviour.EvValueScrollBehaviour;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_CreatPokemonBase;
import com.lanjingzhige.createcobblemon.block.blocks.CB_EvTrainer;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 基础点数（EV）训练器方块实体。
 * <p>
 * 将宝可梦放入方块后，可以通过机械动力的数值设置行为修改其基础点数：
 * <ul>
 *     <li>{@link EvStatBehaviour}：ScrollOptionBehaviour 选项滑块，选择要修改的属性（HP/攻击/防御/特攻/特防/速度）；</li>
 *     <li>{@link EvValueScrollBehaviour}：ScrollValueBehaviour 数字滑条，设置该项属性新的基础点数（0~252）。</li>
 * </ul>
 * 修改不是瞬间完成的：数值滑条记录的是"目标值"，机器需要在旋转供应动力的情况下
 * 花费与改动幅度成正比的一段时间进行训练（{@link #CHANGE_TIME_BASE} + |目标值-当前值| * {@link #CHANGE_TIME_PER_POINT} 工作量，
 * 每 tick 按 {@link #getProcessingSpeed()} 递减，即转速越快训练越快）后才会生效；
 * 机器不旋转或没有宝可梦时暂停计时。
 * <p>
 * 生效时调用 Cobblemon 的 {@link Pokemon#setEV(Stat, int)}，
 * 并写回方块实体 NBT，释放宝可梦后新数值仍然保留。
 */
public class CBE_EvTrainer extends CBE_CreatPokemonBase implements CI_ControlContraption {

    /** 单项基础点数上限（对应 Cobblemon 的 EVs.MAX_STAT_VALUE） */
    public static final int MAX_EV_PER_STAT = 252;
    /** 总基础点数上限（对应 Cobblemon 的 EVs.MAX_TOTAL_VALUE） */
    public static final int MAX_EV_TOTAL = 510;

    /** 训练的基础工作量；改动幅度为 0 时也需要这么多 */
    public static final int CHANGE_TIME_BASE = 100;
    /** 每点基础点数的工作量：|目标值-当前值| 越大，需要的时长越长 */
    public static final int CHANGE_TIME_PER_POINT = 5;

    private static final String KEY_EV_CHANGE_TIMER = "EvChangeTimer";

    /** 属性选择滑块（ScrollOptionBehaviour） */
    public EvStatBehaviour statMode;
    /** 数值滑块（ScrollValueBehaviour），范围为 0~252 */
    public EvValueScrollBehaviour evValue;

    /** 剩余训练工作量；&gt;0 表示训练进行中（滑条显示的是目标值），=0 时滑条即宝可梦当前值 */
    public int changeTimer;

    public CBE_EvTrainer(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        statMode = new EvStatBehaviour(Component.translatable("contraptions.ev_stat_mode"),
                this, new StatBoxTransform());
        // 切换属性：取消当前训练，让数值滑条显示新属性当前的基础点数
        statMode.withCallback(value -> {
            changeTimer = 0;
            syncValueToPokemon();
        });

        evValue = new EvValueScrollBehaviour(Component.translatable("contraptions.ev_value"),
                this, new EvValueBoxTransform());
        evValue.between(0, MAX_EV_PER_STAT);
        evValue.withFormatter(CBE_EvTrainer::formatEv);
        evValue.withCallback(this::onValueChanged);

        behaviours.add(statMode);
        behaviours.add(evValue);
    }

    /** 数字滑条的显示格式，例如 "84" */
    private static String formatEv(int value) {
        return String.valueOf(value);
    }

    /** 当前转动轴提供的转速决定训练速度，1~512 工作单位/tick */
    public int getProcessingSpeed() {
        return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
    }

    /**
     * 玩家把数值滑条改到目标值时调用（服务端）。
     * 先按规则钳制目标值（单项≤252、总和≤510），再按改动幅度安排训练时长。
     */
    private void onValueChanged(int value) {
        if (level == null || level.isClientSide)
            return;
        Pokemon pokemon = getPokemon();
        if (pokemon == null)
            return;

        Stat stat = statOf(statMode.get());
        int current = pokemon.getEvs().getOrDefault(stat);
        int clamped = clampForTotal(pokemon, current, value);
        // 被总量限制修正时，直接把滑条显示值同步过去
        if (clamped != value)
            evValue.value = clamped;

        if (clamped == current) {
            changeTimer = 0;
            setChanged();
            sendData();
            return;
        }

        // 改动幅度越大，需要的时长越长
        changeTimer = CHANGE_TIME_BASE + Math.abs(clamped - current) * CHANGE_TIME_PER_POINT;
        setChanged();
        sendData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        if (changeTimer <= 0)
            return;
        // 没有宝可梦或机器当前未旋转时暂停训练
        if (getPokemon() == null || getSpeed() == 0)
            return;

        changeTimer -= getProcessingSpeed();
        if (changeTimer <= 0) {
            changeTimer = 0;
            applyPendingChange();
        } else {
            setChanged();
        }
    }

    /** 训练完成，把滑条上的目标值真正写入宝可梦 */
    private void applyPendingChange() {
        Pokemon pokemon = getPokemon();
        if (pokemon == null)
            return;

        Stat stat = statOf(statMode.get());
        int current = pokemon.getEvs().getOrDefault(stat);
        int target = evValue.value;
        int clamped = clampForTotal(pokemon, current, target);
        if (clamped != target)
            evValue.value = clamped;

        if (clamped == current) {
            setChanged();
            sendData();
            return;
        }

        pokemon.setEV(stat, clamped);
        // 机器里的 Pokemon 与玩家 PC 中的实例相互独立，必须把变更写回 NBT 才能持久化
        this.pokemonNbt = pokemon.saveToNBT(level.registryAccess(), new CompoundTag());
        setChanged();
        sendData();
    }

    /**
     * 按 Cobblemon 规则钳制目标值：单项不超过 252，六项总和不超过 510。
     * 返回实际可应用的值。
     */
    private static int clampForTotal(Pokemon pokemon, int current, int value) {
        // 还可以使用的总点数 = 510 - 当前总点数
        int remaining = MAX_EV_TOTAL - pokemon.getEvs().total();
        // 单项最多 252，且不能超过"当前值 + 剩余可分配量"
        int maxForStat = Math.max(0, Math.min(MAX_EV_PER_STAT, current + remaining));
        return Mth.clamp(value, 0, maxForStat);
    }

    /** 让数值滑条与当前选中属性的基础点数保持一致（仅在没有待生效的训练时使用） */
    private void syncValueToPokemon() {
        if (evValue == null || statMode == null)
            return;
        Pokemon pokemon = getPokemon();
        if (pokemon == null)
            return;
        int current = pokemon.getEvs().getOrDefault(statOf(statMode.get()));
        if (evValue.value != current) {
            evValue.value = current;
            setChanged();
            sendData();
        }
    }

    private static Stat statOf(MovementEvMode mode) {
        return switch (mode) {
            case EV_HP -> Stats.HP;
            case EV_ATTACK -> Stats.ATTACK;
            case EV_DEFENCE -> Stats.DEFENCE;
            case EV_SPECIAL_ATTACK -> Stats.SPECIAL_ATTACK;
            case EV_SPECIAL_DEFENCE -> Stats.SPECIAL_DEFENCE;
            case EV_SPEED -> Stats.SPEED;
        };
    }

    @Override
    public void setPokemon(ServerPlayer player, UUID uuid, String types) {
        // 基础点数训练器不限制宝可梦的属性
        super.setPokemon(player, uuid, null);
        changeTimer = 0;
        syncValueToPokemon();
        sendData();
    }

    @Override
    public void releasePokemon() {
        super.releasePokemon();
        // 释放时取消未完成的训练，滑条归零
        changeTimer = 0;
        if (evValue != null)
            evValue.value = 0;
        setChanged();
        sendData();
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putInt(KEY_EV_CHANGE_TIMER, changeTimer);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        changeTimer = compound.getInt(KEY_EV_CHANGE_TIMER);
        super.read(compound, registries, clientPacket);
        // 训练进行中时滑条显示的是目标值，不能被覆盖；空闲时同步为宝可梦当前的基础点数
        if (changeTimer <= 0)
            syncValueToPokemon();
    }

    @Override
    protected void ensureClientPokemonEntity(PoseType pose) {
        super.ensureClientPokemonEntity(PoseType.WALK);
    }

    private class StatBoxTransform extends ValueBoxTransform.Sided {
        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(CB_EvTrainer.FACING);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 12, 16.05f);
        }
    }

    private class EvValueBoxTransform extends ValueBoxTransform.Sided {
        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(CB_EvTrainer.FACING);
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 5, 16.05f);
        }
    }
}
