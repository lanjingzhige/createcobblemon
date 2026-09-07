package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.lanjingzhige.createcobblemon.block.blocks.CB_TimeArmLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 时光机大臂方块实体（外圈 16 个，逆时针旋转，无物品槽）。
 */
public class CBE_TimeArmLarge extends CBE_TimeArmBase {

    public CBE_TimeArmLarge(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Class<?> getArmClass() {
        return CB_TimeArmLarge.class;
    }
}
