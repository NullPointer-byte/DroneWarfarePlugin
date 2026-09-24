package ru.millitarytheme.drone;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import ru.millitarytheme.MillitaryTheme;

public final class MQ1PredatorDrone {

    private static final double SPEED = 1.0;
    private static final float SOUND_PITCH = 0.75F;

    private static final int MAX_BOMBS = 8;

    private static final long SOUND_INTERVAL = 20L;

    private int soundTick = 0;

    private final MillitaryTheme plugin;
    private final Player pilot;

    private Phantom phantom;

    private int bombsRemaining = MAX_BOMBS;
    private boolean destroyed;
    private long tickCounter;

    public MQ1PredatorDrone(
            Player pilot,
            Location spawnLocation,
            MillitaryTheme plugin
    ) {
        this.plugin = plugin;
        this.pilot = pilot;

        spawn(spawnLocation);
    }

    private void spawn(Location spawnLocation) {

        Bukkit.getRegionScheduler().execute(
                plugin,
                spawnLocation,
                () -> {

                    if (!pilot.isOnline()) {
                        return;
                    }

                    World world = spawnLocation.getWorld();

                    if (world == null) {
                        return;
                    }

                    phantom = (Phantom) world.spawnEntity(
                            spawnLocation,
                            EntityType.PHANTOM
                    );

                    setupPhantom();

                    phantom.addPassenger(pilot);

                    updateBombsDisplay();

                    startFlight();
                }
        );
    }

    private void setupPhantom() {

        // Размер Predator.
        phantom.setSize(15);

        // Не горит на солнце.
        phantom.setShouldBurnInDay(false);

        // AI НЕ отключаем.
        phantom.setGravity(false);
        phantom.setInvulnerable(false);
        phantom.setSilent(false);
        phantom.setPersistent(true);
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

                    tickCounter++;

                    updateFlight();
                },
                () -> {
                    // Phantom больше недоступен.
                },
                1L,
                1L
        );
    }

    private void updateFlight() {

        Location pilotLocation = pilot.getLocation();

        float yaw = pilotLocation.getYaw();
        float pitch = pilotLocation.getPitch();

        Vector direction = getDirection(
                yaw,
                pitch
        );

        // Predator смотрит туда же,
        // куда смотрит игрок.
        phantom.setRotation(
                yaw,
                pitch
        );

        // Постоянное движение вперёд.
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

    public void dropBomb() {

        if (destroyed) {
            return;
        }

        if (bombsRemaining <= 0) {
            return;
        }

        if (phantom == null || !phantom.isValid()) {
            return;
        }

        Location bombLocation =
                phantom.getLocation()
                        .clone()
                        .add(0, -2.0, 0);

        new PredatorBomb(
                plugin,
                bombLocation,
                pilot
        );

        bombsRemaining--;

        updateBombsDisplay();
    }

    private void updateBombsDisplay() {

        if (!pilot.isOnline()) {
            return;
        }

        pilot.sendActionBar(
                "§7Бомбы: §f"
                        + bombsRemaining
                        + "§7/§f"
                        + MAX_BOMBS
        );
    }

    public int getBombsRemaining() {
        return bombsRemaining;
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

    public void destroy() {

        if (destroyed) {
            return;
        }

        destroyed = true;

        if (phantom != null && phantom.isValid()) {
            phantom.removePassenger(pilot);
            phantom.remove();
        }

        if (pilot.isOnline()) {
            pilot.sendActionBar("");
        }
    }

    public void handleLeftClick() {
        dropBomb();
    }
}