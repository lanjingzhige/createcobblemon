package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.lanjingzhige.createcobblemon.block.TimeMachineLayout;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 时光机臂方块实体的公共父类。
 * 负责找到所属核心、驱动客户端"张开/收拢"展开动画。
 */
public abstract class CBE_TimeArmBase extends SyncedBlockEntity {

    private BlockPos cachedCorePos;
    private int coreFindCooldown;

    /** 客户端展开动画进度：0 = 收拢，1 = 完全张开 */
    public float spread;

    public CBE_TimeArmBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 臂方块对应的方块类型（小臂/大臂） */
    protected abstract Class<?> getArmClass();

    /** 寻找并缓存核心位置（找不到时冷却 20 tick 再试） */
    public BlockPos findCorePos() {
        if (level == null)
            return null;
        if (cachedCorePos != null) {
            if (level.getBlockEntity(cachedCorePos) instanceof CBE_TimeCore)
                return cachedCorePos;
            cachedCorePos = null;
        }
        if (coreFindCooldown > 0) {
            coreFindCooldown--;
            return null;
        }
        cachedCorePos = TimeMachineLayout.findCore(level, worldPosition, getArmClass());
        if (cachedCorePos == null)
            coreFindCooldown = 20;
        return cachedCorePos;
    }

    /** 客户端 tick：跟随核心运行状态做展开/收拢动画 */
    public void tick() {
        if (level == null || !level.isClientSide)
            return;
        boolean runningNow = false;
        BlockPos corePos = findCorePos();
        if (corePos != null && level.getBlockEntity(corePos) instanceof CBE_TimeCore core)
            runningNow = core.running;
        float target = runningNow ? 1f : 0f;
        spread += (target - spread) * 0.08f;
        if (Math.abs(spread - target) < 0.01f)
            spread = target;
    }

    /** 当前臂展（随张开程度从折叠半径插值到全展半径） */
    public float getCurrentRadius(float fullRadius) {
        return Mth.lerp(spread, fullRadius * TimeMachineLayout.ARM_FOLDED_RATIO, fullRadius);
    }
}
