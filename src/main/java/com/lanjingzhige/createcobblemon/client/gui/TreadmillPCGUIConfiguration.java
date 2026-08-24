package com.lanjingzhige.createcobblemon.client.gui;

import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.storage.StorePosition;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.pc.PCGUIConfiguration;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.lanjingzhige.createcobblemon.network.packet.TreadmillPlacePokemonPacket;
import kotlin.Unit;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 跑步机的电脑界面配置（与牧场方块的 PasturePCGUIConfiguration 同模式）。
 * <p>
 * 玩家在电脑界面点击一只宝可梦时，向服务端发送放入跑步机的请求包。
 */
//回调函数，可能会在IDE标红，正常现象
public class TreadmillPCGUIConfiguration extends PCGUIConfiguration {

    private final BlockPos treadmillPos;

    public TreadmillPCGUIConfiguration(BlockPos treadmillPos) {
        super(
            pcgui -> {
                pcgui.closeNormally(true);
                return Unit.INSTANCE;
            },
            (pcgui, position, pokemon) -> {
                if (pokemon != null && !pokemon.isFainted()) {
                    PacketDistributor.sendToServer(new TreadmillPlacePokemonPacket(pokemon.getUuid(), treadmillPos));
                    pcgui.playSound(CobblemonSounds.PC_CLICK);
                }
                return Unit.INSTANCE;
            },
            false,
            pokemon -> !pokemon.isFainted()
        );
        this.treadmillPos = treadmillPos;
    }
}
