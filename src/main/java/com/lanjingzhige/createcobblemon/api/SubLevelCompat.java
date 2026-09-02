package com.lanjingzhige.createcobblemon.api;

import java.util.UUID;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Minimal Sable-free sublevel compatibility shim.
 *
 * <p>The original Create: Biotech teleporter used this class to integrate with
 * Sable sublevels. This port intentionally keeps the same API for the teleporter
 * code, but only supports the outer world: all positions/areas are passed through
 * unchanged.</p>
 */
public final class SubLevelCompat {

    private SubLevelCompat() {}

    @Nullable
    public static Object getContaining(Level level, BlockPos pos) {
        return null;
    }

    @Nullable
    public static UUID getSpaceId(Level level, BlockPos pos) {
        return null;
    }

    public static boolean matchesSpace(Level level, BlockPos pos, @Nullable UUID expectedSubLevelId) {
        return expectedSubLevelId == null;
    }

    public static boolean isValidSpacePosition(Level level, BlockPos pos) {
        return true;
    }

    public static boolean canEntityInteractWith(Level level, BlockPos blockPos, Entity entity) {
        return true;
    }

    public static double distanceSquared(Level level, Position first, Position second) {
        return distanceSquared(level, first.x(), first.y(), first.z(), second.x(), second.y(), second.z());
    }

    public static double distanceSquared(Level level, double firstX, double firstY, double firstZ,
                                         double secondX, double secondY, double secondZ) {
        double dx = firstX - secondX;
        double dy = firstY - secondY;
        double dz = firstZ - secondZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public static Vec3 toWorld(Level level, Position localPos) {
        return new Vec3(localPos.x(), localPos.y(), localPos.z());
    }

    public static Vec3 toWorld(Level level, BlockPos spaceAnchor, Position localPos) {
        return toWorld(level, localPos);
    }

    public static Vec3 toWorld(@Nullable Object subLevel, Position localPos) {
        return new Vec3(localPos.x(), localPos.y(), localPos.z());
    }

    public static AABB toWorldBounds(Level level, BlockPos blockPos, AABB localBounds) {
        return localBounds;
    }

    public static Vec3 toLocal(Level level, BlockPos blockPos, Position worldPos) {
        return new Vec3(worldPos.x(), worldPos.y(), worldPos.z());
    }

    public static Vec3 localOffsetToRenderWorld(Level level, BlockPos blockPos, Vec3 localOffset, float partialTick) {
        return localOffset;
    }

    public static void forEachIncludingEntitySpace(Level level, Position worldOrigin, Entity entity,
                                                   BiConsumer<Object, BlockPos> consumer) {
        BlockPos center = BlockPos.containing(worldOrigin.x(), worldOrigin.y(), worldOrigin.z());
        consumer.accept(null, center);
    }
}
