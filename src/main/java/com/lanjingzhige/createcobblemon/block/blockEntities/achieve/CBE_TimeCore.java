package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormParticlePacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.lanjingzhige.createcobblemon.block.TimeMachineLayout;
import com.lanjingzhige.createcobblemon.recipe.timemachine.ModTimeMachineRecipe;
import com.lanjingzhige.createcobblemon.recipe.timemachine.TimeMachineRecipe;
import com.lanjingzhige.createcobblemon.recipe.timemachine.TimeMachineRecipeInput;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * 时光机核心方块实体。
 * <p>
 * 结构：1 个核心 + 8 个小臂（内圈）+ 16 个大臂（外圈），
 * 其中 6 个小臂是物品臂（对应配方的 6 个输入槽）。
 * <ul>
 *     <li>结构成型且获得动力（转速非 0）时机器的臂展开并开始旋转：小臂顺时针、大臂逆时针（渲染层实现）；</li>
 *     <li>当 6 个物品臂上的物品与自定义配方匹配时开始"召唤"，消耗输入物品；</li>
 *     <li>召唤时长 = 基础工作量 + 每个输入物品附加工作量，转速越快递减得越快（转速越快召唤时间越短）；</li>
 *     <li>召唤期间持续播放宝可梦进化的粒子特效（cobblemon:evo_particles）；</li>
 *     <li>完成后在核心上方召唤一只配方指定的野生宝可梦（默认 70 级）。</li>
 * </ul>
 */
public class CBE_TimeCore extends GeneratingKineticBlockEntity {

    public static final int SUMMON_TIME_BASE = 800;
    public static final int SUMMON_TIME_PER_INPUT = 120;

    /**
     * 每转速每 tick 的旋转角度（度）。
     * 小臂顺时针（负方向）、大臂逆时针（正方向）在渲染层分别取反/取正。
     */
    public static final float SPIN_SPEED_FACTOR = 0.105f;

    /**
     * 终局粒子动画的时长（tick）。
     * 对应 cobblemon:evo_particles 特效的 emitter active_time = 12 秒。
     */
    public static final int FINISH_EFFECT_TICKS = 240;

    private static final String KEY_FORMED = "TimeMachineFormed";
    private static final String KEY_RUNNING = "TimeMachineRunning";
    private static final String KEY_SUMMON_TIMER = "TimeMachineSummonTimer";
    private static final String KEY_SPECIES = "TimeMachineSpecies";
    private static final String KEY_LEVEL = "TimeMachineLevel";
    private static final String KEY_SPIN = "TimeMachineSpin";
    private static final String KEY_FINISH_TICKS = "TimeMachineFinishTicks";

    /** 结构是否完整成型 */
    public boolean formed;
    /** 成型且获得动力；臂的展开/旋转以该状态为准 */
    public boolean running;
    /** 剩余召唤工作量；>0 表示正在召唤 */
    public int summonTimer;
    /** 正在召唤的宝可梦物种 */
    public ResourceLocation speciesId;
    /** 召唤出的宝可梦等级（默认 70） */
    public int summonLevel = 70;
    /** 旋转角度（服务端每 tick 累计并同步，客户端用于渲染臂的旋转） */
    public float spinAngle;
    /** 终局粒子阶段剩余 tick；>0 表示正在等待进化粒子动画播放完毕 */
    public int finishTicks;

    public CBE_TimeCore(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 不需要额外行为；核心本身是 SmartBlockEntity，用于动力网络与同步
    }

    /** 转速决定召唤速度：1~512 工作单位/tick */
    public int getProcessingSpeed() {
        return (int) Math.max(1, Math.min(512, Math.abs(getSpeed() / 16f)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        // 结构检测（每 tick 便宜：24 次方块读取；成型状态变化时通知客户端）
        boolean nowFormed = TimeMachineLayout.isFormed(level, worldPosition);
        if (nowFormed != formed) {
            formed = nowFormed;
            setChanged();
            sendData();
            if (!formed) {
                // 结构被破坏：取消召唤与终局粒子阶段
                summonTimer = 0;
                finishTicks = 0;
                speciesId = null;
            }
        }

        boolean nowRunning = formed && getSpeed() != 0;
        if (nowRunning != running) {
            running = nowRunning;
            setChanged();
            sendData();
        }
        if (!running)
            return;

        // 累计旋转角度并同步给客户端（臂的旋转动画完全由该值驱动，不依赖客户端单独同步转速）
        float prevSpin = spinAngle;
        spinAngle = (spinAngle + getSpeed() * SPIN_SPEED_FACTOR) % 360.0f;
        if (spinAngle != prevSpin)
            sendData();

        if (summonTimer > 0) {
            summonTimer -= getProcessingSpeed();
            if (summonTimer <= 0) {
                summonTimer = 0;
                // 召唤计时完成：进入终局粒子阶段，等进化粒子动画播放完毕再召唤宝可梦
                if (finishTicks <= 0) {
                    finishTicks = FINISH_EFFECT_TICKS;
                    spawnEvolutionParticles();
                }
            } else {
                // 召唤期间定期播放进化粒子特效
                if (level.getGameTime() % 12 == 0)
                    spawnEvolutionParticles();
            }
            setChanged();
            return;
        }

        // 终局粒子阶段：等待动画播完后吐出宝可梦
        if (finishTicks > 0) {
            finishTicks--;
            if (finishTicks <= 0)
                finishSummon();
            setChanged();
            return;
        }

        // 空闲：检查 6 个物品臂上的物品是否匹配配方
        Optional<RecipeHolder<TimeMachineRecipe>> found = ModTimeMachineRecipe.find(buildInput(), level);
        if (found.isPresent() && found.get().value().isValid())
            startSummon(found.get().value());
    }

    /** 从 6 个物品臂读取物品，构造成配方输入（槽位可为空） */
    public TimeMachineRecipeInput buildInput() {
        ItemStack[] stacks = new ItemStack[TimeMachineLayout.ITEM_ARM_COUNT];
        for (int i = 0; i < stacks.length; i++) {
            BlockPos armPos = TimeMachineLayout.getItemArmPos(worldPosition, i);
            stacks[i] = level.getBlockEntity(armPos) instanceof CBE_TimeArmSmall arm ? arm.getItemStack() : ItemStack.EMPTY;
        }
        return new TimeMachineRecipeInput(stacks);
    }

    private void startSummon(TimeMachineRecipe recipe) {
        // 消耗 6 个物品臂上的输入物品
        consumeInputs();
        this.speciesId = recipe.getSpeciesId();
        this.summonLevel = recipe.getLevel();
        this.finishTicks = 0;
        // 改动幅度：输入物品个数越多，需要的时间越长
        this.summonTimer = SUMMON_TIME_BASE + recipe.getInputCount() * SUMMON_TIME_PER_INPUT;
        setChanged();
        sendData();
    }

    private void consumeInputs() {
        for (int i = 0; i < TimeMachineLayout.ITEM_ARM_COUNT; i++) {
            BlockPos armPos = TimeMachineLayout.getItemArmPos(worldPosition, i);
            if (level.getBlockEntity(armPos) instanceof CBE_TimeArmSmall arm)
                arm.consumeItem();
        }
    }

    private void finishSummon() {
        if (speciesId == null) {
            sendData();
            return;
        }
        ResourceLocation id = speciesId;
        int levelOut = summonLevel;
        speciesId = null;

        Species species = PokemonSpecies.INSTANCE.getByIdentifier(id);
        if (species != null && level instanceof ServerLevel serverLevel) {
            Pokemon pokemon = new Pokemon();
            pokemon.setSpecies(species);
            pokemon.setLevel(levelOut);

            PokemonEntity entity = new PokemonEntity(level, pokemon, CobblemonEntities.POKEMON);
            entity.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
            if (serverLevel.addFreshEntity(entity)) {
                // 召唤完成：宝可梦出现时再爆一次粒子
                spawnEvolutionParticles();
            }
        }
        setChanged();
        sendData();
    }

    /** 在核心上方播放宝可梦进化的粒子特效（发送给附近玩家） */
    public void spawnEvolutionParticles() {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        Vec3 pos = Vec3.atCenterOf(worldPosition);
        new SpawnSnowstormParticlePacket(
                ResourceLocation.fromNamespaceAndPath("cobblemon", "evo_particles"), pos)
                .sendToPlayersAround(pos.x, pos.y, pos.z, 64.0, serverLevel.dimension(), player -> false);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        compound.putBoolean(KEY_FORMED, formed);
        compound.putBoolean(KEY_RUNNING, running);
        compound.putInt(KEY_SUMMON_TIMER, summonTimer);
        compound.putInt(KEY_LEVEL, summonLevel);
        compound.putFloat(KEY_SPIN, spinAngle);
        compound.putInt(KEY_FINISH_TICKS, finishTicks);
        if (speciesId != null)
            compound.putString(KEY_SPECIES, speciesId.toString());
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        formed = compound.getBoolean(KEY_FORMED);
        running = compound.getBoolean(KEY_RUNNING);
        summonTimer = compound.getInt(KEY_SUMMON_TIMER);
        summonLevel = compound.contains(KEY_LEVEL) ? compound.getInt(KEY_LEVEL) : 70;
        spinAngle = compound.getFloat(KEY_SPIN);
        finishTicks = compound.getInt(KEY_FINISH_TICKS);
        speciesId = compound.contains(KEY_SPECIES) ? ResourceLocation.parse(compound.getString(KEY_SPECIES)) : null;
        super.read(compound, registries, clientPacket);
    }
}
