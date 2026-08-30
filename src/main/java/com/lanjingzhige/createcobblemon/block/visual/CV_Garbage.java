package com.lanjingzhige.createcobblemon.block.visual;

import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_Garbage;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

public class CV_Garbage extends SingleAxisRotatingVisual<CBE_Garbage> {

    public CV_Garbage(VisualizationContext context, CBE_Garbage blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.partial(AllPartialModels.SHAFTLESS_COGWHEEL));
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
