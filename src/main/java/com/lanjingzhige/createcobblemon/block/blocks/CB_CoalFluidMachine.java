package com.lanjingzhige.createcobblemon.block.blocks;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_CoalFluidMachine;
import com.lanjingzhige.createcobblemon.network.packet.TreadmillOpenScreenPacket;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public class CB_CoalFluidMachine extends HorizontalKineticBlock implements IBE<CBE_CoalFluidMachine>, ICogWheel {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CB_CoalFluidMachine(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        return facing.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() != state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            withBlockEntityDo(level, pos, CBE_CoalFluidMachine::releasePokemon);
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
        if (!level.isClientSide && state.hasBlockEntity() && state.getBlock() != newState.getBlock())
            withBlockEntityDo(level, pos, CBE_CoalFluidMachine::releasePokemon);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public Class<CBE_CoalFluidMachine> getBlockEntityClass() {
        return CBE_CoalFluidMachine.class;
    }

    @Override
    public BlockEntityType<? extends CBE_CoalFluidMachine> getBlockEntityType() {
        return ModBlockEntity.CBE_COAL_FLUID.get();
    }

    @Override
    public boolean isLargeCog() {
        return ICogWheel.super.isLargeCog();
    }

    @Override
    public boolean isSmallCog() {
        return true;
    }
}
