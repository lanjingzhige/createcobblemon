package com.lanjingzhige.createcobblemon.block.blockEntities.achieve;

import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blocks.CB_TimeArmSmall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 时光机小臂方块实体。
 * 每个小臂可以放置 1 组物品，物品会跟随小臂的旋转动画移动；
 * 其中 6 个物品臂（北、东北、东、东南、南、西南）作为配方的输入槽。
 */
public class CBE_TimeArmSmall extends CBE_TimeArmBase {

    private static final String KEY_ITEM = "Item";

    public final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            CBE_TimeArmSmall.this.setChanged();
            CBE_TimeArmSmall.this.sendItemUpdate();
        }
    };

    public CBE_TimeArmSmall(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Class<?> getArmClass() {
        return CB_TimeArmSmall.class;
    }

    public ItemStack getItemStack() {
        return itemHandler.getStackInSlot(0).copy();
    }

    /** 把玩家手中的 1 个物品放到臂上（空位才可放置） */
    public boolean insertItem(ItemStack stack) {
        if (!itemHandler.getStackInSlot(0).isEmpty())
            return false;
        itemHandler.setStackInSlot(0, stack.copyWithCount(1));
        return true;
    }

    /** 取走臂上的物品 */
    public ItemStack takeItem() {
        ItemStack out = getItemStack();
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        return out;
    }

    /** 配方命中后消耗臂上的物品 */
    public void consumeItem() {
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
    }

    private void sendItemUpdate() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    // ---------- 持久化与客户端同步 ----------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(KEY_ITEM, itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(KEY_ITEM))
            itemHandler.deserializeNBT(registries, tag.getCompound(KEY_ITEM));
    }

    @Override
    public CompoundTag writeClient(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(KEY_ITEM, itemHandler.serializeNBT(registries));
        return tag;
    }

    @Override
    public void readClient(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains(KEY_ITEM))
            itemHandler.deserializeNBT(registries, tag.getCompound(KEY_ITEM));
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntity.CBE_TIME_ARM_SMALL.get(),
                (be, context) -> be.itemHandler
        );
    }
}
