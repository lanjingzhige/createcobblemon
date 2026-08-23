package com.lanjingzhige.createcobblemon.block.blockEntities.renderer;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CB_TreadmillEntity;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Treadmill;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 客户端方块实体渲染器：在跑步机上方渲染合成 PokemonEntity 的行走动画。
 * <p>
 * Cobblemon 的行走动画由 {@code q.anim_time} 驱动，动画年龄在方块实体 tick 中
 * 通过 {@code delegate.tick(entity)} 推进，这里只需更新 partialTicks 并交给
 * 实体渲染调度器渲染即可。
 */
public class CB_TreadmillRenderer extends KineticBlockEntityRenderer<CB_TreadmillEntity> {

    public CB_TreadmillRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    protected void renderSafe(CB_TreadmillEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // 与原版渲染路径一致：无 Flywheel 时用“上方空气”的光照渲染传动轴，
        // 避免自身体素因 noOcclusion + lightBlock=1 偏暗而显得漆黑。
        Level level = blockEntity.getLevel();
        int shaftLight = level != null ? LevelRenderer.getLightColor(level, blockEntity.getBlockPos().above()) : packedLight;
        super.renderSafe(blockEntity, partialTick, poseStack, buffer, shaftLight, packedOverlay);

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


        int entityLight = calculateEntityLight(level, entity, blockEntity);

        poseStack.pushPose();
        poseStack.translate(0.5, 1.0, 0.5);
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.render(entity, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, buffer, entityLight);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CB_TreadmillEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0);
    }

    private int calculateEntityLight(Level level, PokemonEntity entity, CB_TreadmillEntity blockEntity) {
        if (level == null) return 15728880;

        BlockPos lightPos = entity.blockPosition();
        int sky = level.getBrightness(LightLayer.SKY, lightPos);
        int block = level.getBrightness(LightLayer.BLOCK, lightPos);

        // 添加环境光遮罩效果（可选）
        int minBlockLight = blockEntity.getLevel().dimensionType().hasFixedTime() ? 4 : 6;
        return LightTexture.pack(sky, Math.max(block, minBlockLight));
    }

    @Override
    protected BlockState getRenderedBlockState(CB_TreadmillEntity be) {
        return shaft(getRotationAxisOf(be));
    }
}
