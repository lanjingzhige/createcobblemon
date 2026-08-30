package com.lanjingzhige.createcobblemon.block.blockEntities;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.block.blocks.CB_Treadmill;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class CBE_CreatPokemonGenerator extends CBE_CreatPokemonBase {

    public CBE_CreatPokemonGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateGeneratedRotation();
    }

    @Override
    public void setPokemon(ServerPlayer player, UUID uuid) {
        super.setPokemon(player, uuid);
        if (!hasPokemon())
            return;
        Pokemon pokemon = getPokemon();
        if (pokemon == null)
            return;
        for (ElementalType type : pokemon.getTypes()) {
            if (type.showdownId().equals("flying")) {
                level.setBlock(worldPosition,
                        getBlockState().setValue(CB_Treadmill.FLY, true),
                        3);
            }
        }
        updateGeneratedRotation();
    }

    @Override
    public void releasePokemon() {
        super.releasePokemon();
        updateGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        Pokemon pokemon = getPokemon();
        if (pokemon == null) {
            return 0.0f;
        }
        return pokemon.getStat(Stats.SPEED);
    }

    @Override
    protected void ensureClientPokemonEntity(PoseType pose) {
        super.ensureClientPokemonEntity(null);
        // 注意：继承自父类的 pokemon 字段在客户端 read() 之后为 null（懒加载设计），
        // 直接访问字段会 NPE；必须通过 getPokemon() 按需从 NBT 加载。

    }
}
