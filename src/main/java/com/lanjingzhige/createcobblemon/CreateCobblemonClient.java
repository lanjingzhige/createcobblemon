package com.lanjingzhige.createcobblemon;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.lanjingzhige.createcobblemon.client.gui.TreadmillPCGUIConfiguration;
import com.lanjingzhige.createcobblemon.network.packet.TreadmillOpenScreenPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@Mod(value = CreateCobblemon.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreateCobblemon.MODID, value = Dist.CLIENT)
public class CreateCobblemonClient {
    public CreateCobblemonClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CreateCobblemon.LOGGER.info("HELLO FROM CLIENT SETUP");
        CreateCobblemon.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }


    public static void handleOpenTreadmill(TreadmillOpenScreenPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPC pc = CobblemonClient.INSTANCE.getStorage().getPcStores().get(payload.pcId());
            if (pc == null)
                return;
            ClientParty party = CobblemonClient.INSTANCE.getStorage().getParty();
            // PCGUI 的 unseenWallpapers 是 Kotlin 非空参数，不能传 null（会触发 Intrinsics.checkNotNullParameter）。
            // 跑步机没有"未查看的壁纸"概念，传空集合即可（Cobblemon 自己传的是 packet.unseenWallpapers.toMutableSet()）。
            PCGUI gui = new PCGUI(pc, party, new TreadmillPCGUIConfiguration(payload.pos()), 0,
                new java.util.HashSet<>());
            Minecraft.getInstance().setScreen(gui);
        });
    }
}
