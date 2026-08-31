package com.lanjingzhige.createcobblemon.block;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.*;
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

    public static void register() {
        // 静态字段在类加载时完成注册
    }
}
