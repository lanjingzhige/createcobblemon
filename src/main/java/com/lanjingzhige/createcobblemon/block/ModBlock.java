package com.lanjingzhige.createcobblemon.block;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Treadmill;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class ModBlock {

    public static final CreateRegistrate REGISTRATE = CreateCobblemon.REGISTRATE;

    /**
     * 跑步机：容量 32 SU/RPM × 转速 8 RPM = 256 SU（复制水车注册模式）。
     * <p>
     * 注意：CStress.setCapacity() 是 Create 内部配置助手，只允许 Create 自己的方块使用，
     * 附属模组必须通过公开 API BlockStressValues.CAPACITIES 注册（onRegister 回调中拿到 Block 实例）。
     */
    public static final BlockEntry<CB_Treadmill> CB_TREADMILL = REGISTRATE.block("cb_treadmill", CB_Treadmill::new)
        .initialProperties(SharedProperties::wooden)
        .properties(p -> p.noOcclusion())
        .transform(axeOrPickaxe())
        .onRegister(BlockStressValues.setGeneratorSpeed(8))
        .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 32))
        .item()
        .transform(customItemModel())
        .register();

    public static void register() {
        // 静态字段在类加载时完成注册
    }
}
