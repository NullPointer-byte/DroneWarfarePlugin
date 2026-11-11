package ru.millitarytheme.drone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
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

    private final MillitaryTheme plugin;

    private final DroneManager manager;

    private final Player pilot;

    private final Location returnLocation;

    private Phantom phantom;

    private ItemDisplay droneDisplay;

    private boolean destroyed;

    public Drone(
            MillitaryTheme plugin,
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

                    /*
                     * AI НЕ отключаем — иначе ломается
                     * управление и пилот слетает.
                     */
                    phantom.setInvulnerable(false);
                    phantom.setSilent(false);
                    phantom.setHealth(8.0);

                    phantom.addPassenger(pilot);

                    /*
                     * Модель — видна ВСЕМ.
                     */
                    spawnDisplay(spawnLocation, world);

                    /*
                     * Phantom — виден ТОЛЬКО пилоту.
                     */
                    hidePhantomFromEveryoneExceptPilot();

                    startFlight();
                }
        );
    }

    private void spawnDisplay(
            Location location,
            World world
    ) {

        droneDisplay = world.spawn(
                location,
                ItemDisplay.class,
                display -> {

                    ItemStack modelItem =
                            new ItemStack(Material.PAPER);

                    ItemMeta meta =
                            modelItem.getItemMeta();

                    if (meta != null) {
                        meta.setItemModel(
                                new NamespacedKey(
                                        "militarytheme",
                                        "drone_model"
                                )
                        );
                        modelItem.setItemMeta(meta);
                    }

                    display.setItemStack(modelItem);

                    display.setTransformation(
                            new Transformation(
                                    new org.joml.Vector3f(0f, 0f, 0f),
                                    new org.joml.Quaternionf(),
                                    new org.joml.Vector3f(1.5f, 1.5f, 1.5f),
                                    new org.joml.Quaternionf()
                            )
                    );

                    /*
                     * По умолчанию модель видна всем.
                     */
                    display.setVisibleByDefault(true);
                    display.setPersistent(false);
                }
        );
    }

    /*
     * Прячем Phantom от ВСЕХ, кроме пилота.
     */
    private void hidePhantomFromEveryoneExceptPilot() {

        if (phantom == null) {
            return;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {

            if (online.equals(pilot)) {
                continue;
            }

            online.getScheduler().run(
                    plugin,
                    task -> {
                        if (phantom != null && phantom.isValid()) {
                            online.hideEntity(plugin, phantom);
                        }
                    },
                    null
            );
        }
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

        if (!phantom.getPassengers().contains(pilot)) {
            phantom.addPassenger(pilot);
        }

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

        syncDisplay(current, yaw, pitch);

        soundTick++;

        if (soundTick >= SOUND_INTERVAL) {

            soundTick = 0;

            World world = phantom.getWorld();

            if (world != null) {
                world.playSound(
                        phantom.getLocation(),
                        Sound.ENTITY_BEE_LOOP,
                        0.8F,
                        SOUND_PITCH
                );
            }
        }
    }

    private void syncDisplay(
            Location location,
            float yaw,
            float pitch
    ) {

        if (droneDisplay == null
                || !droneDisplay.isValid()) {
            return;
        }

        droneDisplay.teleport(location);
        droneDisplay.setRotation(yaw, pitch);
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

    public void explode(Location location) {

        if (destroyed) {
            return;
        }

        destroyed = true;

        manager.unregister(pilot);

        cleanupEntities();

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

                    // Взрыв без разрушения — только урон/эффект.
                    world.createExplosion(
                            location,
                            EXPLOSION_POWER,
                            false,   // setFire
                            false    // breakBlocks
                    );

                    // Своя воронка с логом.
                    ExplosionUtil.makeCrater(
                            plugin,
                            location,
                            6,       // радиус для обычного дрона
                            2,       // maxY
                            "#DroneFlight"
                    );
                }
        );

        returnPilot();
    }

    public void destroy() {

        if (destroyed) {
            return;
        }

        destroyed = true;

        manager.unregister(pilot);

        cleanupEntities();

        returnPilot();
    }

    private void cleanupEntities() {

        if (phantom != null && phantom.isValid()) {

            phantom.removePassenger(pilot);

            phantom.remove();
        }

        if (droneDisplay != null
                && droneDisplay.isValid()) {

            droneDisplay.remove();
        }
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

    private void returnPilot() {

        if (!pilot.isOnline()) {
            return;
        }

        pilot.teleportAsync(
                returnLocation
        );
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