package com.lanjingzhige.createcobblemon.block.renderer;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_CoalFluidMachine;
import com.lanjingzhige.createcobblemon.block.blocks.CB_CoalFluidMachine;
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

public class CR_CoalFluidMachine extends KineticBlockEntityRenderer<CBE_CoalFluidMachine> {

    public CR_CoalFluidMachine(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CBE_CoalFluidMachine blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        super.renderSafe(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);

        PokemonEntity entity = blockEntity.getClientPokemonEntity();
        if (entity == null)
            return;

        ((PokemonClientDelegate) entity.getDelegate()).updatePartialTicks(partialTick);

        Direction facing = blockEntity.getBlockState().getValue(CB_CoalFluidMachine.FACING);
        float yaw = facing.toYRot();
        entity.setYRot(yaw);
        entity.yBodyRot = yaw;
        entity.yHeadRot = yaw;
        entity.yBodyRotO = yaw;
        entity.yHeadRotO = yaw;

        Level level = blockEntity.getLevel();
        int entityLight = LevelRenderer.getLightColor(level, entity.blockPosition());

        poseStack.pushPose();
        poseStack.translate(0.5, 1.0, 0.5);
        Minecraft.getInstance().getEntityRenderDispatcher()
                .render(entity, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, buffer, entityLight);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CBE_CoalFluidMachine blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0);
    }

    @Override
    protected BlockState getRenderedBlockState(CBE_CoalFluidMachine be) {
        return be.getBlockState();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CBE_CoalFluidMachine be, BlockState state) {
        return CachedBuffers.partialFacingVertical(
                AllPartialModels.SHAFTLESS_COGWHEEL, state,
                Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE));
    }
}
