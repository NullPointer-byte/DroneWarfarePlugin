package ru.millitarytheme.drone;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import ru.millitarytheme.ExplosionUtil;
import ru.millitarytheme.MillitaryTheme;

public class PredatorBomb {

    private static final float EXPLOSION_POWER = 18.0f;

    private final Arrow arrow;

    private final Player owner;

    private final MillitaryTheme plugin;

    private TNTPrimed tnt;

    private ScheduledTask updateTask;

    private boolean exploded = false;

    public PredatorBomb(
            MillitaryTheme plugin,
            Location spawnLocation,
            Player owner
    ) {
        this.plugin = plugin;
        this.owner = owner;

        World world = spawnLocation.getWorld();

        if (world == null) {
            throw new IllegalArgumentException(
                    "Bomb location has no world"
            );
        }

        arrow = (Arrow) world.spawnEntity(
                spawnLocation,
                EntityType.ARROW
        );

        setupArrow();
        spawnTntVisual();
        startFlight();
    }

    private void setupArrow() {

        arrow.setGravity(true);

        arrow.setVelocity(
                new Vector(0, -0.15, 0)
        );

        arrow.setPickupStatus(
                Arrow.PickupStatus.DISALLOWED
        );

        arrow.setPersistent(false);
    }

    private void spawnTntVisual() {

        Location location =
                arrow.getLocation().clone();

        World world = location.getWorld();

        if (world == null) {
            return;
        }

        tnt = (TNTPrimed) world.spawnEntity(
                location,
                EntityType.TNT
        );

        // TNT здесь только визуальный.
        tnt.setFuseTicks(Integer.MAX_VALUE);
        tnt.setGravity(false);
        tnt.setSilent(true);
    }

    private void startFlight() {

        updateTask = arrow.getScheduler().runAtFixedRate(
                plugin,

                task -> {

                    if (exploded || arrow.isDead()) {
                        task.cancel();
                        return;
                    }

                    updateTntPosition();

                    checkCollision();

                    playWhistle();
                },

                () -> {
                    // Arrow больше недоступна для выполнения задачи.
                },

                1L,
                1L
        );
    }

    private void updateTntPosition() {

        if (tnt == null || tnt.isDead()) {
            return;
        }

        Location location =
                arrow.getLocation().clone();

        Vector velocity =
                arrow.getVelocity();

        if (velocity.lengthSquared() > 0.001) {

            location.subtract(
                    velocity
                            .clone()
                            .normalize()
                            .multiply(0.6)
            );
        }

        tnt.teleportAsync(location);
    }

    private void checkCollision() {

        Location location =
                arrow.getLocation();

        Block block =
                location.getBlock();

        if (!block.getType().isAir()) {

            explode(location);

            return;
        }

        Location below =
                location.clone().add(
                        0,
                        -0.35,
                        0
                );

        if (!below.getBlock().getType().isAir()) {
            explode(below);
        }
    }

    private void playWhistle() {

        Location location =
                arrow.getLocation();

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        world.playSound(
                location,
                Sound.ENTITY_ARROW_SHOOT,
                0.25f,
                1.8f
        );
    }

    private void explode(Location location) {

        if (exploded) {
            return;
        }

        exploded = true;

        if (updateTask != null) {
            updateTask.cancel();
        }

        if (!arrow.isDead()) {
            arrow.remove();
        }

        if (tnt != null && !tnt.isDead()) {
            tnt.remove();
        }

        createExplosion(location);
    }

    private void createExplosion(Location location) {

        World world =
                location.getWorld();

        if (world == null) {
            return;
        }

        world.createExplosion(
                location,
                EXPLOSION_POWER,
                false,
                false,
                owner
        );

        createDustWave(location);

        makeBomb(location);
    }

    public void makeBomb(Location center) {
        // Ограничим сверху, чтобы не копать лишнего.
        // maxY = +2 от центра — можно настроить.
        ExplosionUtil.makeCrater(
                plugin,
                center,
                18,   // radius
                2,    // maxY
                "#Drone"
        );
    }

    private void createDustWave(Location center) {

        World world =
                center.getWorld();

        if (world == null) {
            return;
        }

        world.spawnParticle(
                Particle.CLOUD,
                center.clone().add(
                        0,
                        0.2,
                        0
                ),
                120,
                5.0,
                0.4,
                5.0,
                0.12
        );

        world.spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                center.clone().add(
                        0,
                        0.5,
                        0
                ),
                40,
                4.0,
                0.5,
                4.0,
                0.05
        );

        world.playSound(
                center,
                Sound.ENTITY_GENERIC_EXPLODE,
                4.0f,
                0.65f
        );
    }

    public void remove() {

        if (updateTask != null) {
            updateTask.cancel();
        }

        if (!arrow.isDead()) {
            arrow.remove();
        }

        if (tnt != null && !tnt.isDead()) {
            tnt.remove();
        }
    }

    public boolean hasExploded() {
        return exploded;
    }

    public Arrow getArrow() {
        return arrow;
    }
}