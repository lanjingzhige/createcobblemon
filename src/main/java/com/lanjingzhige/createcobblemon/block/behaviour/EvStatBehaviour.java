package com.lanjingzhige.createcobblemon.block.behaviour;

import com.lanjingzhige.createcobblemon.block.CI_ControlContraption.MovementEvMode;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 基础点数训练器：选择"要修改哪一项基础点数属性"的选项滑块。
 * <p>
 * 继承 ScrollOptionBehaviour 并覆盖 getType()/netId()，
 * 让同一方块上的多个"数值设置类行为"可以同时共存
 * （SmartBlockEntity 以 BehaviourType 作为 Map 的 key，同一类型只有一个会被保留）。
 * <p>
 * 同时覆盖 write/read 使用独立的 NBT 键：
 * ScrollValueBehaviour 默认把值写入固定的 "ScrollValue" 键，
 * 同一方块实体上的两个滚动行为会互相覆盖，导致属性滑块读到数值滑条的 0~252 并越界。
 */
public class EvStatBehaviour extends ScrollOptionBehaviour<MovementEvMode> {

    public static final BehaviourType<EvStatBehaviour> TYPE = new BehaviourType<>("ev_stat");

    private static final String NBT_KEY = "EvStatValue";

    public EvStatBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(MovementEvMode.class, label, be, slot);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public int netId() {
        return 1;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // 不调用 super：避免把值再写入共享的 "ScrollValue" 键
        nbt.putInt(NBT_KEY, value);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // 防御性钳制，避免旧数据/损坏数据导致 options[value] 越界
        value = Mth.clamp(nbt.getInt(NBT_KEY), 0, max);
    }
}
