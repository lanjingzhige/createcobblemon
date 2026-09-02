package com.lanjingzhige.createcobblemon.mixin.client;

import com.lanjingzhige.createcobblemon.Config;
import com.lanjingzhige.createcobblemon.event.E_FairyTeleporter;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(Vec3 pPos);

    @Inject(method = "setup", at = @At("RETURN"), require = 0)
    private void createcobblemon$lowerFirstPersonViewInShulkerTeleporter(BlockGetter level, Entity entity,
            boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        if (!Config.ENABLE_SHULKER_TELEPORTER_CAMERA_OFFSET.get())
            return;
        if (detached)
            return;
        if (!(entity instanceof Player player))
            return;
        if (player != Minecraft.getInstance().player)
            return;

        Vec3 offset = E_FairyTeleporter.getFirstPersonCameraOffset(player, partialTick);
        if (offset.equals(Vec3.ZERO))
            return;

        setPosition(getPosition().add(offset));
    }
}