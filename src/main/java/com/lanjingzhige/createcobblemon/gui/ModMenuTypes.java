package com.lanjingzhige.createcobblemon.gui;

import com.lanjingzhige.createcobblemon.CreateCobblemon;
import com.lanjingzhige.createcobblemon.gui.menu.CM_FairyTeleporter;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CreateCobblemon.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<CM_FairyTeleporter>> SHULKER_TELEPORTER =
            MENU_TYPES.register("shulker_teleporter", () -> IMenuTypeExtension.create(CM_FairyTeleporter::new));

    private ModMenuTypes() {}

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
