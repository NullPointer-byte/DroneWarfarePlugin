package ru.millitarytheme.drone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import ru.millitarytheme.ExplosionUtil;
import ru.millitarytheme.MillitaryTheme;

public final class Drone {

    private static final double SPEED = 0.8;

    private static final double COLLISION_DISTANCE = 1.5;

    private static final float EXPLOSION_POWER = 8.0F;

    private static final float SOUND_PITCH = 20.00F;

    private static final long SOUND_INTERVAL = 20L;

    private int soundTick = 0;

    private final JavaPlugin plugin;

    private final DroneManager manager;

    private final Player pilot;

    private final Location returnLocation;

    private Phantom phantom;

    private boolean destroyed;

    public Drone(
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

                    phantom = (Phantom) world.spawnEntity(
                            spawnLocation,
                            EntityType.PHANTOM
                    );

                    phantom.setInvulnerable(false);
                    phantom.setSilent(false);
                    phantom.setHealth(8.0);

                    phantom.addPassenger(pilot);

                    startFlight();
                }
        );
    }

    private void startFlight() {

        phantom.getScheduler().runAtFixedRate(
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

                    if (!phantom.isValid()) {
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
                phantom.getLocation();

        Location pilotLocation =
                pilot.getLocation();

        float yaw =
                pilotLocation.getYaw();

        float pitch =
                pilotLocation.getPitch();

        Vector direction =
                getDirection(yaw, pitch);

        if (hasBlockCollision(
                current,
                direction
        )) {
            explode(current);
            return;
        }

        phantom.setRotation(
                yaw,
                pitch
        );

        phantom.setVelocity(
                direction.multiply(SPEED)
        );

        // Поддерживаем непрерывный звук во время полёта.
        soundTick++;

        if (soundTick >= SOUND_INTERVAL) {

            soundTick = 0;

            World world = phantom.getWorld();

            world.playSound(
                    phantom,
                    Sound.ENTITY_BEE_LOOP,
                    0.8F,
                    SOUND_PITCH
            );
        }
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

        return world.rayTraceBlocks(
                location,
                direction,
                COLLISION_DISTANCE,
                org.bukkit.FluidCollisionMode.NEVER,
                true
        ) != null;
    }

    public void explode(Location location) {

        if (destroyed) return;
        destroyed = true;

        manager.unregister(pilot);

        if (phantom != null && phantom.isValid()) {
            phantom.removePassenger(pilot);
            phantom.remove();
        }

        spawnCrashEffect(location);

        World world = location.getWorld();
        if (world != null) {

            // Взрыв без разрушения — только урон/эффект.
            world.createExplosion(
                    location,
                    EXPLOSION_POWER,
                    false,   // setFire
                    false    // breakBlocks
            );

            // Своя воронка с логом.
            ExplosionUtil.makeCrater(
                    (MillitaryTheme) plugin,
                    location,
                    6,       // радиус для обычного дрона
                    2,       // maxY
                    "#DroneFlight"
            );
        }

        returnPilot();
    }

    private void spawnCrashEffect(Location location) {

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

        if (phantom != null && phantom.isValid()) {
            phantom.removePassenger(pilot);
            phantom.remove();
        }

        returnPilot();
    }

    private void returnPilot() {

        if (!pilot.isOnline()) {
            return;
        }

        pilot.teleportAsync(returnLocation);
    }

    public Phantom getPhantom() {
        return phantom;
    }

    public Player getPilot() {
        return pilot;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}