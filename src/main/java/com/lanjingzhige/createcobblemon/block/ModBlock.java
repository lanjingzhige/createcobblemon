package com.lanjingzhige.createcobblemon.block;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.block.blocks.*;
import com.lanjingzhige.createcobblemon.block.blocks.CB_FairyTeleporter;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockBehaviour;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public class ModBlock {

    public static final CreateRegistrate REGISTRATE = CreateCobblemon.REGISTRATE;

    /**
     * 跑步机：容量 32 SU/RPM，转速由宝可梦速度决定（see CB_TreadmillEntity#getGeneratedSpeed），
     * 宝可梦速度越快，提供的应力越高。
     * <p>
     * 注意：CStress.setCapacity() 是 Create 内部配置助手，只允许 Create 自己的方块使用，
     * 附属模组必须通过公开 API BlockStressValues.CAPACITIES 注册（onRegister 回调中拿到 Block 实例）。
     */
    public static final BlockEntry<CB_Treadmill> CB_TREADMILL = REGISTRATE.block("cb_treadmill", CB_Treadmill::new)
        .initialProperties(SharedProperties::wooden)
        .properties(BlockBehaviour.Properties::noOcclusion)
        .transform(axeOrPickaxe())
        .onRegister(BlockStressValues.setGeneratorSpeed(8, true))
        .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 16))
        .item()
        .transform(customItemModel())
        .register();


    public static final BlockEntry<CB_Garbage> CB_GARBAGE = REGISTRATE.block("cb_garbage", CB_Garbage::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 4.0))
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CB_CoalFluidMachine> CB_COAL_FLUID = REGISTRATE.block("cb_coal_fluid", CB_CoalFluidMachine::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 8.0))
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CB_Mulch> CB_MULCH = REGISTRATE.block("cb_mulch", CB_Mulch::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 8.0))
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CB_Ice> CB_ICE = REGISTRATE.block("cb_ice", CB_Ice::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .onRegister(block -> BlockStressValues.CAPACITIES.register(block, () -> 8.0))
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CB_FairyTeleporter> SHULKER_TELEPORTER = REGISTRATE.block("shulker_teleporter", CB_FairyTeleporter::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CB_Ground> CB_GROUND = REGISTRATE.block("cb_ground", CB_Ground::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CB_Rock> CB_ROCK = REGISTRATE.block("cb_rock", CB_Rock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .item()
            .transform(customItemModel())
            .register();

    public static final BlockEntry<CB_Steel> CB_STEEL = REGISTRATE.block("cb_steel", CB_Steel::new)
            .initialProperties(SharedProperties::wooden)
            .properties(BlockBehaviour.Properties::noOcclusion)
            .transform(axeOrPickaxe())
            .item()
            .transform(customItemModel())
            .register();

    public static void register() {
        // 静态字段在类加载时完成注册
    }
}
