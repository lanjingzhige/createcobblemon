package com.lanjingzhige.createcobblemon.network.packet;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * S2C: 服务端 -> 客户端，请求打开跑步机对应的电脑界面。
 *
 * @param pos  跑步机方块位置（点击后回传放置目标）
 * @param pcId 玩家电脑(PCStore)的 UUID，用于客户端定位 ClientPC
 */
public record TreadmillOpenScreenPacket(BlockPos pos, UUID pcId) implements CustomPacketPayload {

    public static final Type<TreadmillOpenScreenPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCobblemon.MODID, "open_treadmill"));

    /** 1.21.1 的 ByteBufCodecs 没有 UUID 编解码器，这里用最符号位/最低有效位各 8 字节手动实现 */
    private static final StreamCodec<ByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
        (buf, uuid) -> buf.writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits()),
        buf -> new UUID(buf.readLong(), buf.readLong()));

    public static final StreamCodec<ByteBuf, TreadmillOpenScreenPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, TreadmillOpenScreenPacket::pos,
        UUID_STREAM_CODEC, TreadmillOpenScreenPacket::pcId,
        TreadmillOpenScreenPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
