package com.lanjingzhige.createcobblemon.block.visual;

import com.lanjingzhige.createcobblemon.block.blockEntities.CB_TreadmillEntity;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

/**
 * 跑步机的传动轴 visual。
 * <p>
 * Create 默认的 {@link com.simibubi.create.content.kinetics.base.ShaftVisual} 在更新光照时
 * 采样方块“自身体素”的光照，而跑步机使用了 noOcclusion，方块自身体素的光常会比相邻空气暗，
 * 导致传动轴渲染成黑色。这里同时采样自身体素和上方体素，并取两者更亮的光照，避免：
 * <ul>
 *     <li>自身体素初始为 0 时轴全黑；</li>
 *     <li>上方被方块遮挡时轴跟着上方一起变黑。</li>
 * </ul>
 */
public class CB_TreadmillShaftVisual extends SingleAxisRotatingVisual<CB_TreadmillEntity> {

    public CB_TreadmillShaftVisual(VisualizationContext context, CB_TreadmillEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFT));
    }

    @Override
    public void updateLight(float partialTick) {
        int sky = 0;
        int block = 0;
        for (BlockPos p : new BlockPos[]{pos, pos.above(), pos.north(), pos.south(), pos.east(), pos.west()}) {
            sky = Math.max(sky, level.getBrightness(LightLayer.SKY, p));
            block = Math.max(block, level.getBrightness(LightLayer.BLOCK, p));
        }
        FlatLit.relight(LightTexture.pack(block, sky), rotatingModel);
    }
}
