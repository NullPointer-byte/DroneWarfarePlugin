package ru.millitarytheme.util;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Phantom;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class DroneEffects {

    private DroneEffects() {
    }

    public static boolean hasBlockCollision(
            Location location,
            Vector direction,
            double distance
    ) {
        World world =
                location.getWorld();

        if (world == null) {
            return true;
        }

        RayTraceResult result =
                world.rayTraceBlocks(
                        location,
                        direction,
                        distance,
                        FluidCollisionMode.NEVER,
                        true
                );

        return result != null;
    }

    public static void playFlightEffects(
            Phantom phantom,
            Vector direction
    ) {
        Location location =
                phantom.getLocation();

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        Vector backwards =
                direction.clone()
                        .normalize()
                        .multiply(-1);

        for (int i = 0; i < 4; i++) {

            Location particleLocation =
                    location.clone().add(
                            backwards.clone()
                                    .multiply(i * 0.35)
                    );

            world.spawnParticle(
                    Particle.FLAME,
                    particleLocation,
                    1,
                    0.05,
                    0.05,
                    0.05,
                    0.01
            );

            world.spawnParticle(
                    Particle.SMOKE,
                    particleLocation,
                    1,
                    0.05,
                    0.05,
                    0.05,
                    0.01
            );
        }

        world.playSound(
                location,
                Sound.ENTITY_PHANTOM_FLAP,
                0.25F,
                0.7F
        );
    }
}