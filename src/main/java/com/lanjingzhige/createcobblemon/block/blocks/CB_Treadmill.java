package com.lanjingzhige.createcobblemon.block.blocks;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.entity.CB_TreadmillEntity;
import com.lanjingzhige.createcobblemon.network.packet.TreadmillOpenScreenPacket;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 跑步机方块：机械动力发电机。
 * <p>
 * 水平放置，宝可梦沿 facing 方向跑步；侧面（垂直于 facing 轴）可接传动轴输出动力。
 * 应力 = 容量 32 SU/RPM × 转速 RPM（转速 = 宝可梦当前速度值，宝可梦越快应力越高）。
 * <p>
 * 交互：
 * 右键      —— 打开电脑界面选择宝可梦放入
 * 潜行右键  —— 释放当前宝可梦，归还其电脑
 */
public class CB_Treadmill extends HorizontalKineticBlock implements IBE<CB_TreadmillEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FLY = BooleanProperty.create("fly");

    public CB_Treadmill(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FLY, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FLY);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        Axis facingAxis = state.getValue(HORIZONTAL_FACING).getAxis();
        return face.getAxis() != facingAxis;
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            // 潜行右键：释放当前宝可梦
            withBlockEntityDo(level, pos, CB_TreadmillEntity::releasePokemon);
            level.setBlock(pos,level.getBlockState(pos).setValue(FLY, false),3);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            PCStore pc = Cobblemon.INSTANCE.getStorage().getPC((ServerPlayer) player);
            PacketDistributor.sendToPlayer((ServerPlayer) player, new TreadmillOpenScreenPacket(pos, pc.getUuid()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 先归还宝可梦，再让 super 清理方块实体与动力网络
        if (!level.isClientSide && state.hasBlockEntity() && state.getBlock() != newState.getBlock())
            withBlockEntityDo(level, pos, CB_TreadmillEntity::releasePokemon);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<CB_TreadmillEntity> getBlockEntityClass() {
        return CB_TreadmillEntity.class;
    }

    @Override
    public BlockEntityType<? extends CB_TreadmillEntity> getBlockEntityType() {
        return ModBlockEntity.CB_TREADMILL_ENTITY.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        return facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

}
