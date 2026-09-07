package com.lanjingzhige.createcobblemon.block.blocks;

import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeArmSmall;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 时光机小臂方块：围绕核心的 8 个内圈方块之一，
 * 每个可以放置 1 组物品（物品跟随小臂旋转）。
 * 手动放置：手持物品右键 = 放上，空手右键 / 潜行右键 = 取下。
 */
public class CB_TimeArmSmall extends Block implements IBE<CBE_TimeArmSmall> {

    public CB_TimeArmSmall(Properties properties) {
        super(properties);
    }

    @Override
    public Class<CBE_TimeArmSmall> getBlockEntityClass() {
        return CBE_TimeArmSmall.class;
    }

    @Override
    public BlockEntityType<? extends CBE_TimeArmSmall> getBlockEntityType() {
        return ModBlockEntity.CBE_TIME_ARM_SMALL.get();
    }

    // 客户端 tick：驱动臂的展开/收拢动画
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide && type == getBlockEntityType())
            return (l, p, s, be) -> ((CBE_TimeArmSmall) be).tick();
        return null;
    }

    /** 空手右键：取走臂上的物品 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, be -> {
                ItemStack out = be.takeItem();
                if (!out.isEmpty() && !player.getInventory().add(out))
                    player.drop(out, false);
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 手持物品右键：把物品放到臂上 */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide)
            return ItemInteractionResult.sidedSuccess(true);
        CBE_TimeArmSmall be = getBlockEntity(level, pos);
        boolean inserted = be != null && be.insertItem(stack);
        if (inserted && !player.isCreative())
            player.getItemInHand(hand).shrink(1);
        return inserted ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 破坏时把臂上的物品掉出来
        if (!level.isClientSide && state.hasBlockEntity() && state.getBlock() != newState.getBlock())
            withBlockEntityDo(level, pos, be -> {
                ItemStack out = be.takeItem();
                if (!out.isEmpty())
                    popResource(level, pos, out);
            });
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
