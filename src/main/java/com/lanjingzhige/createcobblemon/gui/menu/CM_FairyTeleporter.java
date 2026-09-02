package com.lanjingzhige.createcobblemon.gui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_FairyTeleporter;
import com.lanjingzhige.createcobblemon.gui.ModMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CM_FairyTeleporter extends AbstractContainerMenu {

	private final CBE_FairyTeleporter blockEntity;
	private final BlockPos blockPos;
	private final String ownAddress;
	private final String targetAddress;
	private final List<String> candidateAddresses;
	@Nullable
	private final UUID subLevelId;

	public CM_FairyTeleporter(int id, Inventory playerInventory, RegistryFriendlyByteBuf data) {
		this(id, playerInventory, getBlockEntity(playerInventory, data.readBlockPos()), data);
	}

	public CM_FairyTeleporter(int id, Inventory playerInventory, CBE_FairyTeleporter blockEntity) {
		super(ModMenuTypes.SHULKER_TELEPORTER.get(), id);
		this.blockEntity = blockEntity;
		this.blockPos = blockEntity.getBlockPos();
		this.ownAddress = blockEntity.getOwnAddress();
		this.targetAddress = blockEntity.getTargetAddress();
		this.candidateAddresses = List.copyOf(blockEntity.getCandidateAddresses());
		this.subLevelId = blockEntity.getSubLevelId();
	}

	private CM_FairyTeleporter(int id, Inventory playerInventory, CBE_FairyTeleporter blockEntity,
                               RegistryFriendlyByteBuf data) {
		super(ModMenuTypes.SHULKER_TELEPORTER.get(), id);
		this.blockEntity = blockEntity;
		this.blockPos = blockEntity.getBlockPos();
		this.ownAddress = data.readUtf(CBE_FairyTeleporter.MAX_ADDRESS_LENGTH);
		this.targetAddress = data.readUtf(CBE_FairyTeleporter.MAX_ADDRESS_LENGTH);
		this.candidateAddresses = readCandidateAddresses(data);
		this.subLevelId = data.readBoolean() ? data.readUUID() : null;
	}

	@Override
	public boolean stillValid(Player player) {
		return blockEntity.canPlayerUse(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public CBE_FairyTeleporter getBlockEntity() {
		return blockEntity;
	}

	public String getOwnAddress() {
		return ownAddress;
	}

	public String getTargetAddress() {
		return targetAddress;
	}

	public List<String> getCandidateAddresses() {
		return candidateAddresses;
	}

	@Nullable
	public UUID getSubLevelId() {
		return subLevelId;
	}

	private static List<String> readCandidateAddresses(RegistryFriendlyByteBuf data) {
		int size = data.readVarInt();
		List<String> addresses = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
			addresses.add(data.readUtf(CBE_FairyTeleporter.MAX_ADDRESS_LENGTH));
		return List.copyOf(addresses);
	}

	private static CBE_FairyTeleporter getBlockEntity(Inventory playerInventory, BlockPos pos) {
		BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
		if (blockEntity instanceof CBE_FairyTeleporter teleporter)
			return teleporter;
		throw new IllegalStateException("Shulker Teleporter menu opened without a matching block entity");
	}
}
