package ru.millitarytheme.drone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class FpvDrone {

    private static final double SPEED = 1.35;

    private static final double COLLISION_DISTANCE = 1.5;

    private static final float EXPLOSION_POWER = 5.0F;

    private static final long SOUND_INTERVAL = 8L;

    private static final float SOUND_PITCH = 3.35F;

    private final JavaPlugin plugin;

    private final DroneManager manager;

    private final Player pilot;

    private final Location returnLocation;

    private Bat bat;

    private boolean destroyed;

    private long soundTick;

    public FpvDrone(
            JavaPlugin plugin,
            DroneManager manager,
            Player pilot
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.pilot = pilot;
        this.returnLocation = pilot.getLocation().clone();
    }

    public void spawn() {

        Location spawnLocation =
                pilot.getLocation()
                        .clone()
                        .add(0, 1.5, 0);

        Bukkit.getRegionScheduler().execute(
                plugin,
                spawnLocation,
                () -> {

                    if (!pilot.isOnline()) {
                        manager.unregister(pilot);
                        return;
                    }

                    World world =
                            spawnLocation.getWorld();

                    if (world == null) {
                        manager.unregister(pilot);
                        return;
                    }

                    bat = (Bat) world.spawnEntity(
                            spawnLocation,
                            EntityType.BAT
                    );

                    /*
                     * AI НЕ отключаем.
                     */
                    bat.setInvulnerable(false);
                    bat.setSilent(false);
                    bat.setAwake(true);

                    /*
                     * Увеличиваем летучую мышь.
                     */
                    if (bat.getAttribute(
                            Attribute.SCALE
                    ) != null) {

                        bat.getAttribute(
                                Attribute.SCALE
                        ).setBaseValue(2.5);
                    }

                    /*
                     * Игрок управляет мышью.
                     */
                    bat.addPassenger(pilot);

                    startFlight();
                }
        );
    }

    private void startFlight() {

        bat.getScheduler().runAtFixedRate(
                plugin,
                task -> {

                    if (destroyed) {
                        task.cancel();
                        return;
                    }

                    if (!pilot.isOnline()) {
                        task.cancel();
                        destroy();
                        return;
                    }

                    if (!bat.isValid()) {
                        task.cancel();
                        destroy();
                        return;
                    }

                    updateFlight();
                },
                () -> {},
                1L,
                1L
        );
    }

    private void updateFlight() {

        Location current =
                bat.getLocation();

        Location pilotLocation =
                pilot.getLocation();

        float yaw =
                pilotLocation.getYaw();

        float pitch =
                pilotLocation.getPitch();

        Vector direction =
                getDirection(yaw, pitch);

        /*
         * Shift — зависание.
         */
        if (pilot.isSneaking()) {

            bat.setVelocity(
                    new Vector(0, 0, 0)
            );

            playFlightSound(current);

            spawnIdleParticles(current);

            return;
        }

        /*
         * Проверяем блок перед мышью.
         */
        if (hasBlockCollision(
                current,
                direction
        )) {

            explode(current);

            return;
        }

        /*
         * Управляем полётом через velocity.
         * AI при этом остаётся включённым.
         */
        bat.setRotation(
                yaw,
                pitch
        );

        bat.setVelocity(
                direction.multiply(SPEED)
        );

        spawnFlightParticles(
                current,
                direction
        );

        playFlightSound(current);
    }

    private Vector getDirection(
            float yaw,
            float pitch
    ) {

        double yawRadians =
                Math.toRadians(yaw);

        double pitchRadians =
                Math.toRadians(pitch);

        double x =
                -Math.sin(yawRadians)
                        * Math.cos(pitchRadians);

        double y =
                -Math.sin(pitchRadians);

        double z =
                Math.cos(yawRadians)
                        * Math.cos(pitchRadians);

        return new Vector(
                x,
                y,
                z
        ).normalize();
    }

    private boolean hasBlockCollision(
            Location location,
            Vector direction
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
                        COLLISION_DISTANCE,
                        org.bukkit.FluidCollisionMode.NEVER,
                        true
                );

        return result != null;
    }

    private void playFlightSound(Location location) {
    if (soundTick++ % SOUND_INTERVAL != 0) {
        return;
    }

    World world = location.getWorld();

    if (world == null) {
        return;
    }

    world.playSound(
            location,
            Sound.ENTITY_BEE_LOOP,
            0.8F,
            SOUND_PITCH
    );
}

    private void spawnFlightParticles(
            Location location,
            Vector direction
    ) {

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        Location trail =
                location.clone().add(
                        direction.clone().multiply(-0.5)
                );

        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                trail,
                2,
                0.08,
                0.08,
                0.08,
                0.01
        );

        world.spawnParticle(
                Particle.SMOKE,
                trail,
                1,
                0.04,
                0.04,
                0.04,
                0.01
        );
    }

    private void spawnIdleParticles(
            Location location
    ) {

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                location,
                1,
                0.15,
                0.08,
                0.15,
                0.01
        );
    }

    public void explode(Location location) {

        if (destroyed) {
            return;
        }

        destroyed = true;

        manager.unregister(pilot);

        if (bat != null && bat.isValid()) {

            bat.removePassenger(pilot);

            bat.remove();
        }

        spawnCrashEffect(location);

        Bukkit.getRegionScheduler().execute(
                plugin,
                location,
                () -> {

                    World world =
                            location.getWorld();

                    if (world == null) {
                        return;
                    }

                    world.createExplosion(
                            location,
                            EXPLOSION_POWER,
                            false,
                            true
                    );
                }
        );

        returnPilot();
    }

    private void spawnCrashEffect(
            Location location
    ) {

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        for (int y = 0; y < 10; y++) {

            Location particleLocation =
                    location.clone().add(
                            0,
                            y * 0.45,
                            0
                    );

            world.spawnParticle(
                    Particle.CAMPFIRE_COSY_SMOKE,
                    particleLocation,
                    7,
                    0.45,
                    0.15,
                    0.45,
                    0.02
            );

            world.spawnParticle(
                    Particle.ASH,
                    particleLocation,
                    10,
                    0.65,
                    0.15,
                    0.65,
                    0.03
            );
        }

        world.spawnParticle(
                Particle.LARGE_SMOKE,
                location,
                25,
                0.8,
                0.4,
                0.8,
                0.04
        );

        world.playSound(
                location,
                Sound.ENTITY_GENERIC_EXPLODE,
                1.2F,
                0.8F
        );
    }

    public void destroy() {

        if (destroyed) {
            return;
        }

        destroyed = true;

        manager.unregister(pilot);

        if (bat != null && bat.isValid()) {

            bat.removePassenger(pilot);

            bat.remove();
        }

        returnPilot();
    }

    private void returnPilot() {

        if (!pilot.isOnline()) {
            return;
        }

        pilot.teleportAsync(
                returnLocation
        );
    }

    public Bat getBat() {
        return bat;
    }

    public Player getPilot() {
        return pilot;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}