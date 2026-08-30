package com.lanjingzhige.createcobblemon.block.renderer;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_Treadmill;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Treadmill;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 客户端方块实体渲染器：在跑步机上方渲染合成 PokemonEntity 的行走动画。
 * <p>
 * Cobblemon 的行走动画由 {@code q.anim_time} 驱动，动画年龄在方块实体 tick 中
 * 通过 {@code delegate.tick(entity)} 推进，这里只需更新 partialTicks 并交给
 * 实体渲染调度器渲染即可。
 * <p>
 * 光照：按原版惯例在宝可梦实际所处位置（跑步机上方的空气格）采样亮度，
 * 使用 {@link LevelRenderer#getLightColor} 取得打包光照，
 * 不做任何人为的亮度提升或环境光兜底。
 */
public class CB_TreadmillRenderer extends KineticBlockEntityRenderer<CBE_Treadmill> {

    public CB_TreadmillRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CBE_Treadmill blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // 传动轴沿用传入的 packedLight，正常渲染
        super.renderSafe(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);

        PokemonEntity entity = blockEntity.getClientPokemonEntity();
        if (entity == null)
            return;

        // 让动画年龄与当前帧的插值对齐
        ((PokemonClientDelegate) entity.getDelegate()).updatePartialTicks(partialTick);

        // 让宝可梦面向方块 facing 方向
        Direction facing = blockEntity.getBlockState().getValue(CB_Treadmill.FACING);
        float yaw = facing.toYRot();
        entity.setYRot(yaw);
        entity.yBodyRot = yaw;
        entity.yHeadRot = yaw;
        entity.yBodyRotO = yaw;
        entity.yHeadRotO = yaw;

        // 按原版实体惯例：在宝可梦真实位置（方块上方一格的空气）采样光照
        Level level = blockEntity.getLevel();
        int entityLight = LevelRenderer.getLightColor(level, entity.blockPosition());

        poseStack.pushPose();
        poseStack.translate(0.5, 1.0, 0.5);
        Minecraft.getInstance().getEntityRenderDispatcher()
                .render(entity, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, buffer, entityLight);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CBE_Treadmill blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0);
    }

    @Override
    protected BlockState getRenderedBlockState(CBE_Treadmill be) {
        return shaft(getRotationAxisOf(be));
    }
}
