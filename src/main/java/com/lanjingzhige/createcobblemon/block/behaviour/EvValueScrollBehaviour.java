package com.lanjingzhige.createcobblemon.block.behaviour;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 基础点数训练器：具体数值（0~252）的数字滑块。
 * <p>
 * 继承 ScrollValueBehaviour 并覆盖 getType()/netId()，
 * 让同一方块上的多个"数值设置类行为"可以同时共存
 * （SmartBlockEntity 以 BehaviourType 作为 Map 的 key，同一类型只有一个会被保留）。
 * <p>
 * 同时覆盖 write/read 使用独立的 NBT 键：
 * ScrollValueBehaviour 默认把值写入固定的 "ScrollValue" 键，
 * 同一方块实体上的两个滚动行为会互相覆盖，导致属性滑块读到数值滑条的 0~252 并越界。
 */
public class EvValueScrollBehaviour extends ScrollValueBehaviour {

    public static final BehaviourType<EvValueScrollBehaviour> TYPE = new BehaviourType<>("ev_value");

    private static final String NBT_KEY = "EvValueValue";

    public EvValueScrollBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public int netId() {
        return 2;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // 不调用 super：避免把值再写入共享的 "ScrollValue" 键
        nbt.putInt(NBT_KEY, value);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // 防御性钳制
        value = Mth.clamp(nbt.getInt(NBT_KEY), 0, max);
    }
}
