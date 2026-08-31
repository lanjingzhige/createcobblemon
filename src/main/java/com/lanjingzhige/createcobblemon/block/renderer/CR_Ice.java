package com.lanjingzhige.createcobblemon.block.renderer;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_Ice;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_Mulch;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Ice;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Mulch;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CR_Ice extends KineticBlockEntityRenderer<CBE_Ice> {

    public CR_Ice(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CBE_Ice blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // 传动轴沿用传入的 packedLight，正常渲染
        super.renderSafe(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);

        PokemonEntity entity = blockEntity.getClientPokemonEntity();
        if (entity == null)
            return;

        // 让动画年龄与当前帧的插值对齐
        ((PokemonClientDelegate) entity.getDelegate()).updatePartialTicks(partialTick);

        // 让宝可梦面向方块 facing 方向
        Direction facing = blockEntity.getBlockState().getValue(CB_Ice.FACING);
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
    public AABB getRenderBoundingBox(CBE_Ice blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0);
    }

    @Override
    protected BlockState getRenderedBlockState(CBE_Ice be) {
        return be.getBlockState();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CBE_Ice be, BlockState state) {
        return CachedBuffers.partialFacingVertical(
                AllPartialModels.SHAFTLESS_COGWHEEL, state,
                Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE));
    }
}