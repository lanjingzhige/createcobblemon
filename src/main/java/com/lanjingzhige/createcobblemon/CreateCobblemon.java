package com.lanjingzhige.createcobblemon;

import com.lanjingzhige.createcobblemon.block.ModBlock;
import com.lanjingzhige.createcobblemon.block.ModBlockEntity;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.*;
import com.lanjingzhige.createcobblemon.block.blockEntities.achieve.CBE_TimeArmSmall;
import com.lanjingzhige.createcobblemon.api.CBMultiBlockLifecycle;
import com.lanjingzhige.createcobblemon.network.TreadmillPackets;
import com.lanjingzhige.createcobblemon.recipe.ModRecipe;
import com.lanjingzhige.createcobblemon.recipe.timemachine.ModTimeMachineRecipe;
import com.lanjingzhige.createcobblemon.gui.ModMenuTypes;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(CreateCobblemon.MODID)
public class CreateCobblemon {
    public static final String MODID = "createcobblemon";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID);

    static {
        REGISTRATE.defaultCreativeTab(CreativeModeTabs.BUILDING_BLOCKS);

        REGISTRATE.setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item))));

        REGISTRATE.addRawLang("createaddon.behaviour.beaker.fill_level", "Beaker fill");
        REGISTRATE.addRawLang("createaddon.behaviour.measuring_cylinder.fill_level", "Cylinder fill");
        REGISTRATE.addRawLang("createaddon.value_settings.fill", "Fill");
    }


    public CreateCobblemon(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        modEventBus.addListener(TreadmillPackets::register);
        // 注意：这里不是监听事件，而是把配方注册表挂到 Mod 事件总线上（与 ModRecipe.register 相同用法）
        ModTimeMachineRecipe.register(modEventBus);


        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        REGISTRATE.registerEventListeners(modEventBus);
        ModBlock.register();
        ModBlockEntity.register();
        ModRecipe.register(modEventBus);
        ModMenuTypes.register(modEventBus);




        modEventBus.addListener(CBE_Garbage::registerCapabilities);
        modEventBus.addListener(CBE_CoalFluidMachine::registerCapabilities);
        modEventBus.addListener(CBE_Mulch::registerCapabilities);
        modEventBus.addListener(CBE_Ice::registerCapabilities);
        modEventBus.addListener(CBE_Ground::registerCapabilities);
        modEventBus.addListener(CBE_Rock::registerCapabilities);
        modEventBus.addListener(CBE_Steel::registerCapabilities);
        modEventBus.addListener(CBE_TimeArmSmall::registerCapabilities);

    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CBMultiBlockLifecycle.registerMovementChecks());

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    public static ResourceLocation modLoc(String path){
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
