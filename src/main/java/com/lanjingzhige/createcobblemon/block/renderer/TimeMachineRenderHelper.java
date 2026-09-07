package com.lanjingzhige.createcobblemon.block.renderer;

import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 时光机臂动画的共享计算与渲染。
 * <p>
 * 旋转角度由核心方块实体（服务端）按转速逐 tick 累计并通过 NBT 同步，
 * 客户端在这里用动画帧的 partial tick 做帧间插值，保证平滑且不依赖客户端单独同步转速。
 * <p>
 * 臂条使用原版 BlockRenderDispatcher.renderSingleBlock + Create 的黄铜外壳方块模型渲染
 * （通过 pose 缩放拉伸成臂条），不依赖 PartialModel 的烘焙时机，渲染不可能静默失败。
 */
public final class TimeMachineRenderHelper {

    /** 臂条截面厚度（块） */
    public static final float ARM_BAR_THICKNESS = 0.25f;

    private TimeMachineRenderHelper() {}

    /** 当前动画帧机器累计的旋转角度（度），负转速会反向累积 */
    public static float getSpin(CBE_TimeCore core) {
        if (core.getLevel() == null)
            return 0;
        float partialTicks = AnimationTickHolder.getPartialTicks(core.getLevel());
        return core.spinAngle + core.getSpeed() * CBE_TimeCore.SPIN_SPEED_FACTOR * partialTicks;
    }

    /** 臂相对核心的方向角（度）。使 +X 小臂在 spin=0 时从核心指向该臂位置 */
    public static float getBaseAngle(int dx, int dz) {
        return (float) Math.toDegrees(Math.atan2(-dz, dx));
    }

    /**
     * 臂尖（物品悬浮处）在渲染器局部坐标系中的偏移。
     * <p>
     * 方块实体渲染器收到的 PoseStack 已经以"该方块实体自己的方块位置"为原点
     * （LevelRenderer 在调用前 translate 了 blockPos），因此这里必须返回
     * 相对 origin（臂方块实体）的坐标，否则臂条/物品会被画到世界坐标处而完全不可见。
     */
    public static Vec3 getArmTip(CBE_TimeCore core, BlockPos origin, float angleDeg, float radius, double height) {
        double a = Math.toRadians(angleDeg);
        BlockPos corePos = core.getBlockPos();
        return new Vec3(
                corePos.getX() - origin.getX() + 0.5 + Math.cos(a) * radius,
                corePos.getY() - origin.getY() + height,
                corePos.getZ() - origin.getZ() + 0.5 - Math.sin(a) * radius);
    }

    /**
     * 渲染臂条：从核心中心向外伸出 radius 格，绕核心旋转 angle 度。
     * 所有坐标都是相对 origin（臂方块实体）的局部坐标（见 {@link #getArmTip}）。
     */
    public static void renderArmBar(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay,
                                    BlockPos corePos, BlockPos origin, float angle, float radius) {
        BlockState barState = AllBlocks.BRASS_CASING.get().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(corePos.getX() - origin.getX() + 0.5, corePos.getY() - origin.getY() + 0.5,
                corePos.getZ() - origin.getZ() + 0.5);
        TransformStack.of(poseStack).rotateYDegrees(angle);
        poseStack.scale(radius, ARM_BAR_THICKNESS, ARM_BAR_THICKNESS);
        poseStack.translate(0, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(barState, poseStack, buffer, light, overlay);
        poseStack.popPose();
    }
}

