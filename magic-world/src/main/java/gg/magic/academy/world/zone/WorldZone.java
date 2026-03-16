package gg.magic.academy.world.zone;

public record WorldZone(
        String id,
        String name,
        String world,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        String enterMessage,
        String mobTable,
        String ambientEffect
) {}
