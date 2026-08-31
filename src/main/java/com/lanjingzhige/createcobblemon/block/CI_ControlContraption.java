package com.lanjingzhige.createcobblemon.block;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.lang.Lang;

public interface CI_ControlContraption {

    static enum MovementIceMode implements INamedIconOptions {

        MOVE_ICE(AllIcons.I_MOVE_PLACE),
        MOVE_PACKED_ICE(AllIcons.I_MOVE_PLACE_RETURNED),
        MOVE_BLUE_ICE(AllIcons.I_MOVE_NEVER_PLACE),
        ;

        private String translationKey;
        private AllIcons icon;

        private MovementIceMode(AllIcons icon) {
            this.icon = icon;
            translationKey = "create.contraptions.movement_ice_mode." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

    }
}
