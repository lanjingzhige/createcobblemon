package com.lanjingzhige.createcobblemon.network.packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_FairyTeleporter;
import com.lanjingzhige.createcobblemon.gui.menu.CM_FairyTeleporter;
import com.lanjingzhige.createcobblemon.api.SubLevelCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class FairyTeleporterConfigPacket implements CustomPacketPayload {

    public static final Type<FairyTeleporterConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateCobblemon.MODID, "shulker_teleporter_config"));

    public static final StreamCodec<FriendlyByteBuf, FairyTeleporterConfigPacket> STREAM_CODEC =
            StreamCodec.<FriendlyByteBuf, FairyTeleporterConfigPacket>of(
                    (buffer, packet) -> packet.write(buffer),
                    FairyTeleporterConfigPacket::new);

    private final BlockPos pos;
    private final String ownAddress;
    private final String targetAddress;
    private final List<String> candidateAddresses;
    @Nullable
    private final UUID subLevelId;

    public FairyTeleporterConfigPacket(BlockPos pos, String ownAddress, String targetAddress,
                                       List<String> candidateAddresses, @Nullable UUID subLevelId) {
        this.pos = pos;
        this.ownAddress = ownAddress;
        this.targetAddress = targetAddress;
        this.candidateAddresses = CBE_FairyTeleporter.normalizeCandidateAddresses(candidateAddresses);
        this.subLevelId = subLevelId;
    }

    public FairyTeleporterConfigPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        ownAddress = buffer.readUtf(CBE_FairyTeleporter.MAX_ADDRESS_LENGTH);
        targetAddress = buffer.readUtf(CBE_FairyTeleporter.MAX_ADDRESS_LENGTH);
        int size = buffer.readVarInt();
        if (size < 0 || size > CBE_FairyTeleporter.MAX_CANDIDATE_ADDRESSES)
            throw new IllegalArgumentException("Invalid Shulker Teleporter candidate address count " + size);
        List<String> addresses = new ArrayList<>(size);
        for (int i = 0; i < size; i++)
            addresses.add(buffer.readUtf(CBE_FairyTeleporter.MAX_ADDRESS_LENGTH));
        candidateAddresses = CBE_FairyTeleporter.normalizeCandidateAddresses(addresses);
        subLevelId = buffer.readBoolean() ? buffer.readUUID() : null;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(ownAddress, CBE_FairyTeleporter.MAX_ADDRESS_LENGTH);
        buffer.writeUtf(targetAddress, CBE_FairyTeleporter.MAX_ADDRESS_LENGTH);
        buffer.writeVarInt(candidateAddresses.size());
        for (String candidateAddress : candidateAddresses)
            buffer.writeUtf(candidateAddress, CBE_FairyTeleporter.MAX_ADDRESS_LENGTH);
        buffer.writeBoolean(subLevelId != null);
        if (subLevelId != null)
            buffer.writeUUID(subLevelId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FairyTeleporterConfigPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                payload.handle(player);
        });
    }

    public void handle(ServerPlayer player) {
        if (player == null)
            return;
        if (!(player.containerMenu instanceof CM_FairyTeleporter menu)
                || !menu.getBlockPos().equals(pos)
                || !Objects.equals(menu.getSubLevelId(), subLevelId))
            return;
        Level level = player.level();
        if (!level.isLoaded(pos) || !SubLevelCompat.matchesSpace(level, pos, subLevelId))
            return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CBE_FairyTeleporter teleporter)
                || menu.getBlockEntity() != teleporter
                || !teleporter.canPlayerUse(player))
            return;
        teleporter.setConfiguration(ownAddress, targetAddress, candidateAddresses);
    }
}
