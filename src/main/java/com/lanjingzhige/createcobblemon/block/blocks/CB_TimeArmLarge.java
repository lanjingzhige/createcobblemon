package com.lanjingzhige.createcobblemon.block.blocks;

import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeArmLarge;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 时光机大臂方块：围绕核心外圈 16 个，无物品槽，逆时针旋转（渲染层动画）。
 */
public class CB_TimeArmLarge extends Block implements IBE<CBE_TimeArmLarge> {

    public CB_TimeArmLarge(Properties properties) {
        super(properties);
    }

    @Override
    public Class<CBE_TimeArmLarge> getBlockEntityClass() {
        return CBE_TimeArmLarge.class;
    }

    @Override
    public BlockEntityType<? extends CBE_TimeArmLarge> getBlockEntityType() {
        return ModBlockEntity.CBE_TIME_ARM_LARGE.get();
    }

    // 客户端 tick：驱动臂的展开/收拢动画
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide && type == getBlockEntityType())
            return (l, p, s, be) -> ((CBE_TimeArmLarge) be).tick();
        return null;
    }
}
