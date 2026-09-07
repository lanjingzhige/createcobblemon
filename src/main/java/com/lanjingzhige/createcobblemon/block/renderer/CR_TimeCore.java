package com.lanjingzhige.createcobblemon.block.renderer;

import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeCore;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 时光机核心渲染：中心大齿轮（与 cb_rock 相同的方式渲染旋转部件）。
 */
public class CR_TimeCore extends KineticBlockEntityRenderer<CBE_TimeCore> {

    public CR_TimeCore(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CBE_TimeCore be, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight, int packedOverlay) {
        super.renderSafe(be, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(CBE_TimeCore blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.5);
    }

    @Override
    protected BlockState getRenderedBlockState(CBE_TimeCore be) {
        return be.getBlockState();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CBE_TimeCore be, BlockState state) {
        return CachedBuffers.partialFacingVertical(
                AllPartialModels.SHAFTLESS_COGWHEEL, state,
                Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE));
    }
}
