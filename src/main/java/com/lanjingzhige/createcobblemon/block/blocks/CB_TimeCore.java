package com.lanjingzhige.createcobblemon.block.blocks;

import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.TimeMachineLayout;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeCore;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 时光机核心方块：5x5 多方块结构的中心，旋转轴在小臂/大臂所在的轴（水平方向），
 * 通过机械动力网络提供转速来驱动时光机（转速越快召唤时间越短）。
 */
public class CB_TimeCore extends HorizontalKineticBlock implements IBE<CBE_TimeCore>, ICogWheel {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CB_TimeCore(Properties properties) {
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
        return InteractionResult.PASS;
    }

    /**
     * 手持小臂/大臂右键核心：自动把臂摆放到最近的空槽位，
     * 不需要手动逐个对准位置（小臂按 北、东北、东、东南、南、西南、西、西北 顺序填补）。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof BlockItem blockItem))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Block held = blockItem.getBlock();
        Class<?> armClass = null;
        if (held instanceof CB_TimeArmSmall)
            armClass = CB_TimeArmSmall.class;
        else if (held instanceof CB_TimeArmLarge)
            armClass = CB_TimeArmLarge.class;
        if (armClass == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.sidedSuccess(true);

        BlockPos target = TimeMachineLayout.findVacantArmSlot(level, pos, armClass);
        if (target == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlockState placeState = held.defaultBlockState();
        level.setBlock(target, placeState, Block.UPDATE_ALL);
        level.playSound(null, target, SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        if (!player.isCreative())
            stack.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public Class<CBE_TimeCore> getBlockEntityClass() {
        return CBE_TimeCore.class;
    }

    @Override
    public BlockEntityType<? extends CBE_TimeCore> getBlockEntityType() {
        return ModBlockEntity.CBE_TIME_CORE.get();
    }

    @Override
    public boolean isSmallCog() {
        return true;
    }
}
