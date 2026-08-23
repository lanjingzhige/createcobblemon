package com.lanjingzhige.createcobblemon.network.packet;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * C2S: 客户端 -> 服务端，玩家在电脑界面中选中了一只宝可梦，请求放入跑步机。
 *
 * @param pokemonUuid 宝可梦的 UUID（在玩家电脑中查找）
 * @param pos         跑步机方块位置
 */
public record TreadmillPlacePokemonPacket(UUID pokemonUuid, BlockPos pos) implements CustomPacketPayload {

    public static final Type<TreadmillPlacePokemonPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCobblemon.MODID, "place_pokemon"));

    /** 1.21.1 的 ByteBufCodecs 没有 UUID 编解码器，这里用最符号位/最低有效位各 8 字节手动实现 */
    private static final StreamCodec<ByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
        (buf, uuid) -> buf.writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits()),
        buf -> new UUID(buf.readLong(), buf.readLong()));

    public static final StreamCodec<ByteBuf, TreadmillPlacePokemonPacket> STREAM_CODEC = StreamCodec.composite(
        UUID_STREAM_CODEC, TreadmillPlacePokemonPacket::pokemonUuid,
        BlockPos.STREAM_CODEC, TreadmillPlacePokemonPacket::pos,
        TreadmillPlacePokemonPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
