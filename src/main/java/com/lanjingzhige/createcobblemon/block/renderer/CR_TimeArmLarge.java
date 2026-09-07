package com.lanjingzhige.createcobblemon.block.renderer;

import com.lanjingzhige.createcobblemon.block.TimeMachineLayout;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeArmLarge;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeCore;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/**
 * 时光机大臂渲染：臂条从核心中心伸出（张开后臂展 = 2 格）围绕核心逆时针旋转。
 */
public class CR_TimeArmLarge implements BlockEntityRenderer<CBE_TimeArmLarge> {

    public CR_TimeArmLarge(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CBE_TimeArmLarge be, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       MultiBufferSource buffer, int light, int overlay) {
        if (be.getLevel() == null)
            return;
        BlockPos corePos = be.findCorePos();
        if (corePos == null)
            return;
        if (!(be.getLevel().getBlockEntity(corePos) instanceof CBE_TimeCore core))
            return;

        int dx = be.getBlockPos().getX() - corePos.getX();
        int dz = be.getBlockPos().getZ() - corePos.getZ();

        float spin = TimeMachineRenderHelper.getSpin(core);
        float angle = TimeMachineRenderHelper.getBaseAngle(dx, dz) + spin; // 大臂：逆时针
        float radius = be.getCurrentRadius(TimeMachineLayout.LARGE_ARM_RADIUS);

        // 注意：渲染器的 PoseStack 已以臂方块自身位置为原点，必须传局部 origin
        TimeMachineRenderHelper.renderArmBar(poseStack, buffer, light, overlay, corePos, be.getBlockPos(), angle, radius);
    }

    @Override
    public boolean shouldRenderOffScreen(CBE_TimeArmLarge blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(CBE_TimeArmLarge blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(4.0);
    }
}
