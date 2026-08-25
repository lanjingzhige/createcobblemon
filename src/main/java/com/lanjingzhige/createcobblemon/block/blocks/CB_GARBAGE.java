package com.lanjingzhige.createcobblemon.block.blocks;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_Treadmill;
import com.lanjingzhige.createcobblemon.network.packet.TreadmillOpenScreenPacket;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public class CB_GARBAGE extends HorizontalKineticBlock implements IBE<CBE_Treadmill> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CB_GARBAGE(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        Direction.Axis facingAxis = state.getValue(HORIZONTAL_FACING).getAxis();
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
            withBlockEntityDo(level, pos, CBE_Treadmill::releasePokemon);
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
            withBlockEntityDo(level, pos, CBE_Treadmill::releasePokemon);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<CBE_Treadmill> getBlockEntityClass() {
        return CBE_Treadmill.class;
    }

    @Override
    public BlockEntityType<? extends CBE_Treadmill> getBlockEntityType() {
        return ModBlockEntity.CB_TREADMILL_ENTITY.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        return facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

}