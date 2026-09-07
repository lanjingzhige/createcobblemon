package com.lanjingzhige.createcobblemon.block;

import com.lanjingzhige.createcobblemon.api.CBMultiBlockLifecycle;
import com.lanjingzhige.createcobblemon.block.blocks.CB_TimeArmLarge;
import com.lanjingzhige.createcobblemon.block.blocks.CB_TimeArmSmall;
import com.lanjingzhige.createcobblemon.block.blocks.CB_TimeCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/**
 * 时光机多方块结构布局（5x5 水平面，核心在中心）：
 * <ul>
 *     <li>核心方块 (0,0)：机械动力动力学方块，负责驱动与召唤流程；</li>
 *     <li>8 个小臂：核心外圈 3x3 的 8 个位置（切比雪夫距离 1）；</li>
 *     <li>16 个大臂：最外圈 5x5 的 16 个位置（切比雪夫距离 2）。</li>
 * </ul>
 * 其中 6 个小臂（北、东北、东、东南、南、西南）是"物品臂"，
 * 对应配方的 6 个输入槽（不足 6 个时其余槽留空）。
 */
public final class TimeMachineLayout {

    private TimeMachineLayout() {}

    /** 小臂相对核心的偏移（顺序即标签顺序，用于渲染角度） */
    public static final BlockPos[] SMALL_ARM_OFFSETS = {
            new BlockPos(0, 0, -1),   // 0 北
            new BlockPos(1, 0, -1),   // 1 东北
            new BlockPos(1, 0, 0),    // 2 东
            new BlockPos(1, 0, 1),    // 3 东南
            new BlockPos(0, 0, 1),    // 4 南
            new BlockPos(-1, 0, 1),   // 5 西南
            new BlockPos(-1, 0, 0),   // 6 西
            new BlockPos(-1, 0, -1),  // 7 西北
    };

    /** 大臂相对核心的偏移（切比雪夫距离 2 的全部 16 个位置） */
    public static final BlockPos[] LARGE_ARM_OFFSETS = {
            new BlockPos(2, 0, -2), new BlockPos(2, 0, -1), new BlockPos(2, 0, 0), new BlockPos(2, 0, 1),
            new BlockPos(2, 0, 2), new BlockPos(1, 0, 2), new BlockPos(0, 0, 2), new BlockPos(-1, 0, 2),
            new BlockPos(-2, 0, 2), new BlockPos(-2, 0, 1), new BlockPos(-2, 0, 0), new BlockPos(-2, 0, -1),
            new BlockPos(-2, 0, -2), new BlockPos(-1, 0, -2), new BlockPos(0, 0, -2), new BlockPos(1, 0, -2),
    };

    /** 配方输入槽数量 */
    public static final int ITEM_ARM_COUNT = 6;

    /** 小臂臂展（从核心中心到小臂方块中心的格数） */
    public static final float SMALL_ARM_RADIUS = 1.0f;
    /** 大臂臂展 */
    public static final float LARGE_ARM_RADIUS = 2.0f;

    /** 收缩（折叠）时臂展占全展的比例 */
    public static final float ARM_FOLDED_RATIO = 0.4f;

    public static boolean isSmallArmOffset(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz)) == 1;
    }

    public static boolean isLargeArmOffset(int dx, int dz) {
        return Math.max(Math.abs(dx), Math.abs(dz)) == 2;
    }

    public static boolean isSmallArmOffset(BlockPos pos) {
        return isSmallArmOffset(pos.getX(), pos.getZ());
    }

    public static boolean isLargeArmOffset(BlockPos pos) {
        return isLargeArmOffset(pos.getX(), pos.getZ());
    }

    /** 第 index 个物品臂（配方输入槽）对应的绝对位置 */
    public static BlockPos getItemArmPos(BlockPos corePos, int index) {
        return corePos.offset(SMALL_ARM_OFFSETS[index]);
    }

    /**
     * 检查以 corePos 为核心的多方块结构是否完整。
     * 未加载区块中的位置视为完好（multi-block 常见约定）。
     */
    public static boolean isFormed(LevelReader level, BlockPos corePos) {
        for (BlockPos offset : SMALL_ARM_OFFSETS) {
            BlockPos pos = corePos.offset(offset);
            if (!CBMultiBlockLifecycle.isLoaded(level, pos))
                continue;
            if (!(level.getBlockState(pos).getBlock() instanceof CB_TimeArmSmall))
                return false;
        }
        for (BlockPos offset : LARGE_ARM_OFFSETS) {
            BlockPos pos = corePos.offset(offset);
            if (!CBMultiBlockLifecycle.isLoaded(level, pos))
                continue;
            if (!(level.getBlockState(pos).getBlock() instanceof CB_TimeArmLarge))
                return false;
        }
        return true;
    }

    /** 检查某臂方块相对核心的位置与其类型是否匹配（用于臂方块寻找核心） */
    public static boolean isArmOffsetValidFor(BlockPos armPos, BlockPos corePos, Class<?> armClass) {
        BlockPos rel = armPos.subtract(corePos);
        if (rel.getY() != 0)
            return false;
        if (armClass == CB_TimeArmSmall.class)
            return isSmallArmOffset(rel.getX(), rel.getZ());
        if (armClass == CB_TimeArmLarge.class)
            return isLargeArmOffset(rel.getX(), rel.getZ());
        return false;
    }

    /** 从臂方块出发寻找核心：在 5x5 范围内遍历可能的核心位置 */
    public static BlockPos findCore(LevelReader level, BlockPos armPos, Class<?> armClass) {        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0)
                    continue;
                BlockPos candidate = armPos.offset(dx, 0, dz);
                if (!CBMultiBlockLifecycle.isLoaded(level, candidate))
                    continue;
                if (level.getBlockState(candidate).getBlock() instanceof CB_TimeCore
                        && isArmOffsetValidFor(armPos, candidate, armClass)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * 为对应类型的臂寻找第一个空的合法槽位（供"手持臂右键核心自动摆放"使用）。
     * 小臂按 北→东北→东→东南→南→西南→西→西北 的顺序填充（前 6 个正好是物品臂），大臂按外圈顺时针。
     */
    public static BlockPos findVacantArmSlot(LevelReader level, BlockPos corePos, Class<?> armClass) {
        BlockPos[] offsets = armClass == CB_TimeArmSmall.class ? SMALL_ARM_OFFSETS : LARGE_ARM_OFFSETS;
        for (BlockPos offset : offsets) {
            BlockPos pos = corePos.offset(offset);
            if (!CBMultiBlockLifecycle.isLoaded(level, pos))
                continue;
            if (level.getBlockState(pos).isAir())
                return pos;
        }
        return null;
    }
}
