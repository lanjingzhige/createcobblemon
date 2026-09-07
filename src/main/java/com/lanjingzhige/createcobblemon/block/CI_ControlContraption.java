package com.lanjingzhige.createcobblemon.block;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.lang.Lang;

public interface CI_ControlContraption {

    enum MovementIceMode implements INamedIconOptions {

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
    enum MovementGroundMode implements INamedIconOptions {

        MOVE_DIRT(AllIcons.I_MOVE_PLACE),
        MOVE_SAND(AllIcons.I_MOVE_PLACE_RETURNED),
        MOVE_CLAY(AllIcons.I_MOVE_NEVER_PLACE),
        ;

        private String translationKey;
        private AllIcons icon;

        private MovementGroundMode(AllIcons icon) {
            this.icon = icon;
            translationKey = "create.contraptions.movement_ground_mode." + Lang.asId(name());
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
    enum MovementRockMode implements INamedIconOptions {

        MOVE_COBBLESTONE(AllIcons.I_MOVE_PLACE),
        MOVE_SMOOTH_STONE(AllIcons.I_MOVE_PLACE_RETURNED),
        MOVE_COBBLED_DEEPSLATE(AllIcons.I_MOVE_NEVER_PLACE),
        MOVE_POLISHED_DEEPSLATE(AllIcons.I_MOVE_PLACE),
        MOVE_ANDESITE(AllIcons.I_MOVE_PLACE_RETURNED),
        ;

        private String translationKey;
        private AllIcons icon;

        private MovementRockMode(AllIcons icon) {
            this.icon = icon;
            translationKey = "create.contraptions.movement_rock_mode." + Lang.asId(name());
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

    enum MovementSteelMode implements INamedIconOptions {

        MOVE_DEEPSLATE_COPPER_ORE(AllIcons.I_MOVE_PLACE),
        MOVE_DEEPSLATE_IRON_ORE(AllIcons.I_MOVE_PLACE_RETURNED),
        MOVE_DEEPSLATE_GOLD_ORE(AllIcons.I_MOVE_NEVER_PLACE),
        MOVE_DEEPSLATE_ZINC_ORE(AllIcons.I_MOVE_PLACE),
        ;

        private String translationKey;
        private AllIcons icon;

        private MovementSteelMode(AllIcons icon) {
            this.icon = icon;
            translationKey = "create.contraptions.movement_steel_mode." + Lang.asId(name());
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

    /**
     * 基础点数（EV）训练器可选的目标属性。
     * 与 MovementRockMode 等枚举一样，用于 ScrollOptionBehaviour 生成的可滑动选项滑块。
     */
    enum MovementEvMode implements INamedIconOptions {

        EV_HP(AllIcons.I_FILL),
        EV_ATTACK(AllIcons.I_TARGET),
        EV_DEFENCE(AllIcons.I_CONFIG_LOCKED),
        EV_SPECIAL_ATTACK(AllIcons.I_ROTATE_CCW),
        EV_SPECIAL_DEFENCE(AllIcons.I_CONFIG_UNLOCKED),
        EV_SPEED(AllIcons.I_3x3),
        ;

        private String translationKey;
        private AllIcons icon;

        private MovementEvMode(AllIcons icon) {
            this.icon = icon;
            translationKey = "create.contraptions.movement_ev_mode." + Lang.asId(name());
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
