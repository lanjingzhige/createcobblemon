package com.lanjingzhige.createcobblemon.block.renderer;

import com.lanjingzhige.createcobblemon.block.TimeMachineLayout;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeArmSmall;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeCore;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 时光机小臂渲染：臂条从核心中心伸出（张开后臂展 = 1 格）围绕核心顺时针旋转；
 * 臂上的物品跟随臂尖位置悬浮移动。
 */
public class CR_TimeArmSmall implements BlockEntityRenderer<CBE_TimeArmSmall> {

    public CR_TimeArmSmall(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CBE_TimeArmSmall be, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int light, int overlay) {
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
        float angle = TimeMachineRenderHelper.getBaseAngle(dx, dz) - spin; // 小臂：顺时针
        float radius = be.getCurrentRadius(TimeMachineLayout.SMALL_ARM_RADIUS);

        // 注意：渲染器的 PoseStack 已以臂方块自身位置为原点，必须传局部 origin
        TimeMachineRenderHelper.renderArmBar(poseStack, buffer, light, overlay, corePos, be.getBlockPos(), angle, radius);

        // 物品跟随臂尖
        ItemStack stack = be.getItemStack();
        if (!stack.isEmpty()) {
            Vec3 tip = TimeMachineRenderHelper.getArmTip(core, be.getBlockPos(), angle, radius, 0.9);
            poseStack.pushPose();
            poseStack.translate(tip.x, tip.y, tip.z);
            TransformStack.of(poseStack).rotateYDegrees(angle);
            poseStack.scale(0.35f, 0.35f, 0.35f);
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poseStack, buffer,
                    be.getLevel(), 0);
            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CBE_TimeArmSmall blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(CBE_TimeArmSmall blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(4.0);
    }
}
