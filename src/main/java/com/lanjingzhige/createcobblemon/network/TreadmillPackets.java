package com.lanjingzhige.createcobblemon.network;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.CreateCobblemonClient;
import com.lanjingzhige.createcobblemon.block.blockEntities.CBE_Treadmill;
import com.lanjingzhige.createcobblemon.network.packet.TreadmillOpenScreenPacket;
import com.lanjingzhige.createcobblemon.network.packet.TreadmillPlacePokemonPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class TreadmillPackets {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CreateCobblemon.MODID).versioned("1");

        // 服务端 -> 客户端：打开跑步机电脑界面（处理器在客户端专用类中）
        registrar.playToClient(
                TreadmillOpenScreenPacket.TYPE,
                TreadmillOpenScreenPacket.STREAM_CODEC,
            CreateCobblemonClient::handleOpenTreadmill);

        // 客户端 -> 服务端：把电脑中的宝可梦放入跑步机
        registrar.playToServer(
                TreadmillPlacePokemonPacket.TYPE,
                TreadmillPlacePokemonPacket.STREAM_CODEC,
            TreadmillPackets::handlePlacePokemon);
    }

    private static void handlePlacePokemon(TreadmillPlacePokemonPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // IPayloadContext.player() 返回的是 Player，需要 instanceof 收窄到 ServerPlayer
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                // 防御：校验距离，防止远程注入
                if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64 * 64)
                    return;
                if (level.getBlockEntity(payload.pos()) instanceof CBE_Treadmill blockEntity)
                    blockEntity.setPokemon(player, payload.pokemonUuid());
            }
        });
    }
}
