package com.lanjingzhige.createcobblemon.block;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.block.blockEntities.entity.CB_TreadmillEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.renderer.CB_TreadmillRenderer;
import com.lanjingzhige.createcobblemon.block.blockEntities.visual.CB_TreadmillShaftVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class ModBlockEntity {

    public static final CreateRegistrate REGISTRATE = CreateCobblemon.REGISTRATE;

    public static final BlockEntityEntry<CB_TreadmillEntity> CB_TREADMILL_ENTITY =
        REGISTRATE.blockEntity("cb_treadmill", CB_TreadmillEntity::new)
            // Flywheel visual：在旋转轴上渲染传动轴（与 renderer 的 getRenderedBlockState 一致）。
            // renderNormally = true 必须保留：CB_TreadmillRenderer 还要负责渲染宝可梦，
            // 设为 false 会连宝可梦一起消失。
            .visual(() -> CB_TreadmillShaftVisual::new, true)
            .validBlocks(ModBlock.CB_TREADMILL)
            .renderer(() -> CB_TreadmillRenderer::new)
            .register();

    public static void register() {
        // 静态字段在类加载时完成注册
    }
}
