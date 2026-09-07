package com.lanjingzhige.createcobblemon.block;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.*;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_FairyTeleporter;
import com.lanjingzhige.createcobblemon.block.renderer.CR_FairyTeleporter;
import com.lanjingzhige.createcobblemon.block.renderer.*;
import com.lanjingzhige.createcobblemon.block.visual.CB_TreadmillShaftVisual;
import com.lanjingzhige.createcobblemon.block.visual.CV_Garbage;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class ModBlockEntity {

    public static final CreateRegistrate REGISTRATE = CreateCobblemon.REGISTRATE;

    public static final BlockEntityEntry<CBE_Treadmill> CBE_TREADMILL =
        REGISTRATE.blockEntity("cbe_treadmill", CBE_Treadmill::new)
            // Flywheel visual：在旋转轴上渲染传动轴（与 renderer 的 getRenderedBlockState 一致）。
            // renderNormally = true 必须保留：CB_TreadmillRenderer 还要负责渲染宝可梦，
            // 设为 false 会连宝可梦一起消失。
            .visual(() -> CB_TreadmillShaftVisual::new, true)
            .validBlocks(ModBlock.CB_TREADMILL)
            .renderer(() -> CB_TreadmillRenderer::new)
            .register();

    public static final BlockEntityEntry<CBE_Garbage> CBE_GARBAGE =
            REGISTRATE.blockEntity("cbe_garbage", CBE_Garbage::new)
                    // Flywheel visual：在旋转轴上渲染传动轴（与 renderer 的 getRenderedBlockState 一致）。
                    // renderNormally = true 必须保留：CB_TreadmillRenderer 还要负责渲染宝可梦，
                    // 设为 false 会连宝可梦一起消失。
                    .visual(() -> CV_Garbage::new, true)
                    .validBlocks(ModBlock.CB_GARBAGE)
                    .renderer(() -> CR_Garbage::new)
                    .register();

    public static final BlockEntityEntry<CBE_CoalFluidMachine> CBE_COAL_FLUID =
            REGISTRATE.blockEntity("cbe_coal_fluid", CBE_CoalFluidMachine::new)
                    .validBlocks(ModBlock.CB_COAL_FLUID)
                    .renderer(() -> CR_CoalFluidMachine::new)
                    .register();

    public static final BlockEntityEntry<CBE_Mulch> CBE_MULCH =
            REGISTRATE.blockEntity("cbe_mulch", CBE_Mulch::new)
                    .validBlocks(ModBlock.CB_MULCH)
                    .renderer(() -> CR_Mulch::new)
                    .register();

    public static final BlockEntityEntry<CBE_Ice> CBE_ICE =
            REGISTRATE.blockEntity("cbe_ice", CBE_Ice::new)
                    .validBlocks(ModBlock.CB_ICE)
                    .renderer(() -> CR_Ice::new)
                    .register();

    public static final BlockEntityEntry<CBE_FairyTeleporter> CBE_SHULKER_TELEPORTER =
            REGISTRATE.blockEntity("shulker_teleporter", CBE_FairyTeleporter::new)
                    .validBlocks(ModBlock.SHULKER_TELEPORTER)
                    .renderer(() -> CR_FairyTeleporter::new)
                    .register();

    public static final BlockEntityEntry<CBE_Ground> CBE_GROUND =
            REGISTRATE.blockEntity("cbe_ground", CBE_Ground::new)
                    .validBlocks(ModBlock.CB_GROUND)
                    .renderer(() -> CR_Ground::new)
                    .register();

    public static final BlockEntityEntry<CBE_Rock> CBE_ROCK =
            REGISTRATE.blockEntity("cbe_rock", CBE_Rock::new)
                    .validBlocks(ModBlock.CB_ROCK)
                    .renderer(() -> CR_Rock::new)
                    .register();

    public static final BlockEntityEntry<CBE_Steel> CBE_STEEL =
            REGISTRATE.blockEntity("cbe_steel", CBE_Steel::new)
                    .validBlocks(ModBlock.CB_STEEL)
                    .renderer(() -> CR_Steel::new)
                    .register();

    public static final BlockEntityEntry<CBE_EvTrainer> CBE_EV_TRAINER =
            REGISTRATE.blockEntity("cbe_ev_trainer", CBE_EvTrainer::new)
                    .validBlocks(ModBlock.CB_EV_TRAINER)
                    .renderer(() -> CR_EvTrainer::new)
                    .register();

    public static final BlockEntityEntry<CBE_TimeCore> CBE_TIME_CORE =
            REGISTRATE.blockEntity("cbe_time_core", CBE_TimeCore::new)
                    .validBlocks(ModBlock.CB_TIME_CORE)
                    .renderer(() -> CR_TimeCore::new)
                    .register();

    public static final BlockEntityEntry<CBE_TimeArmSmall> CBE_TIME_ARM_SMALL =
            REGISTRATE.blockEntity("cbe_time_arm_small", CBE_TimeArmSmall::new)
                    .validBlocks(ModBlock.CB_TIME_ARM_SMALL)
                    .renderer(() -> CR_TimeArmSmall::new)
                    .register();

    public static final BlockEntityEntry<CBE_TimeArmLarge> CBE_TIME_ARM_LARGE =
            REGISTRATE.blockEntity("cbe_time_arm_large", CBE_TimeArmLarge::new)
                    .validBlocks(ModBlock.CB_TIME_ARM_LARGE)
                    .renderer(() -> CR_TimeArmLarge::new)
                    .register();


    public static void register() {
        // 静态字段在类加载时完成注册
    }
}
