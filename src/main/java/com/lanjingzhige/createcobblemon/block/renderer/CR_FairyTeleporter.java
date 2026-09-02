package com.lanjingzhige.createcobblemon.block.renderer;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_FairyTeleporter;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_Ground;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Ground;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.world.phys.AABB;

public class CR_FairyTeleporter extends KineticBlockEntityRenderer<CBE_FairyTeleporter> {

	private static final float LOWER_SHELL_Y = -2.0f;
	private static final float MIXER_IDLE_HEAD_OFFSET = 7 / 16f;
	private static final float FULL_SPIN_DEGREES = 720.0f;

	private final ShulkerModel<Shulker> model;

	public CR_FairyTeleporter(BlockEntityRendererProvider.Context context) {
		super(context);
		model = new ShulkerModel<>(context.bakeLayer(ModelLayers.SHULKER));
	}

	@Override
	protected void renderSafe(CBE_FairyTeleporter be, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		VertexConsumer vertexConsumer = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION.buffer(bufferSource,
			RenderType::entityCutoutNoCull);
		float progress = be.getClosingProgress(partialTick);
		float topY = be.getTopShellYOffset(partialTick);
		float spin = progress * FULL_SPIN_DEGREES;

		renderMixerBody(be, poseStack, bufferSource, packedLight, packedOverlay);
		renderDriveCog(be, poseStack, bufferSource, packedLight);
		renderMixerPole(be, poseStack, bufferSource, packedLight, topY);
		renderBase(poseStack, vertexConsumer, packedLight, packedOverlay);
		renderLid(poseStack, vertexConsumer, packedLight, packedOverlay, topY, spin);


        PokemonEntity entity = be.getClientPokemonEntity();
        if (entity == null)
            return;

        // 让动画年龄与当前帧的插值对齐
        ((PokemonClientDelegate) entity.getDelegate()).updatePartialTicks(partialTick);

        // 让宝可梦面向方块 facing 方向
        Direction facing = be.getBlockState().getValue(CB_Ground.FACING);
        float yaw = facing.toYRot();
        entity.setYRot(yaw);
        entity.yBodyRot = yaw;
        entity.yHeadRot = yaw;
        entity.yBodyRotO = yaw;
        entity.yHeadRotO = yaw;

        // 按原版实体惯例：在宝可梦真实位置（方块上方一格的空气）采样光照
        Level level = be.getLevel();
        int entityLight = LevelRenderer.getLightColor(level, entity.blockPosition());

        poseStack.pushPose();
        poseStack.translate(0.5, 1.0, 0.5);
        Minecraft.getInstance().getEntityRenderDispatcher()
                .render(entity, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, bufferSource, entityLight);
        poseStack.popPose();
	}

	private void renderMixerBody(CBE_FairyTeleporter be, PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight,
                                 int packedOverlay) {
		poseStack.pushPose();
		Minecraft.getInstance()
			.getBlockRenderer()
			.renderSingleBlock(AllBlocks.MECHANICAL_MIXER.getDefaultState(), poseStack, bufferSource, packedLight,
				packedOverlay);
		poseStack.popPose();
	}

	private void renderDriveCog(CBE_FairyTeleporter be, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight) {
		BlockState blockState = be.getBlockState();
		SuperByteBuffer cog = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(cog, be, packedLight)
			.renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
	}

	private void renderMixerPole(CBE_FairyTeleporter be, PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, float topShellY) {
		BlockState blockState = be.getBlockState();
		CachedBuffers.partial(AllPartialModels.MECHANICAL_MIXER_POLE, blockState)
			.translate(0, topShellY - CBE_FairyTeleporter.TOP_SHELL_OPEN_Y - MIXER_IDLE_HEAD_OFFSET, 0)
			.light(packedLight)
			.renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
	}

	private void renderBase(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
		ModelPart lid = model.getLid();
		resetModel();

		poseStack.pushPose();
		poseStack.translate(0.0d, LOWER_SHELL_Y, 0.0d);
		applyShulkerBoxPose(poseStack);
		for (ModelPart part : model.parts()) {
			if (part != lid)
				part.render(poseStack, vertexConsumer, packedLight, packedOverlay);
		}
		poseStack.popPose();
	}

	private void renderLid(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
		float yOffset, float spinDegrees) {
		ModelPart lid = model.getLid();
		resetModel();
		lid.setPos(0.0f, 24.0f, 0.0f);
		lid.yRot = (float) Math.toRadians(spinDegrees);

		poseStack.pushPose();
		poseStack.translate(0.0d, yOffset, 0.0d);
		applyShulkerBoxPose(poseStack);
		lid.render(poseStack, vertexConsumer, packedLight, packedOverlay);
		poseStack.popPose();
	}

	private void resetModel() {
		for (ModelPart part : model.parts())
			part.resetPose();
		model.getHead().resetPose();
	}

	private static void applyShulkerBoxPose(PoseStack poseStack) {
		poseStack.translate(0.5f, 0.5f, 0.5f);
		poseStack.scale(0.9995f, 0.9995f, 0.9995f);
		poseStack.mulPose(Direction.UP.getRotation());
		poseStack.scale(1.0f, -1.0f, -1.0f);
		poseStack.translate(0.0f, -1.0f, 0.0f);
	}

    @Override
    public AABB getRenderBoundingBox(CBE_FairyTeleporter blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0);
    }

    @Override
    protected BlockState getRenderedBlockState(CBE_FairyTeleporter be) {
        return be.getBlockState();
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CBE_FairyTeleporter be, BlockState state) {
        return CachedBuffers.partialFacingVertical(
                AllPartialModels.SHAFTLESS_COGWHEEL, state,
                Direction.fromAxisAndDirection(getRotationAxisOf(be), Direction.AxisDirection.POSITIVE));
    }
}
