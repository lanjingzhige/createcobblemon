package com.lanjingzhige.createcobblemon.block.blockEntities.visual;

import com.lanjingzhige.createcobblemon.block.blockEntities.CB_TreadmillEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;

/**
 * 跑步机的传动轴 visual。
 * <p>
 * Create 默认的 {@link com.simibubi.create.content.kinetics.base.ShaftVisual} 在更新光照时
 * 采样方块“自身体素”的光照，而跑步机使用了 noOcclusion，方块自身体素的光常会比相邻空气暗，
 * 导致传动轴渲染成黑色。这里改为采样方块上方的空气光照，使轴与周围方块/空气的观感一致。
 */
public class CB_TreadmillShaftVisual extends SingleAxisRotatingVisual<CB_TreadmillEntity> {

    public CB_TreadmillShaftVisual(VisualizationContext context, CB_TreadmillEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFT));
    }

    @Override
    public void updateLight(float partialTick) {
        // 采样上方体素的光，避免自身体素因 noOcclusion + lightBlock=1 而偏暗/全黑。
        relight(pos.above(), rotatingModel);
    }
}
