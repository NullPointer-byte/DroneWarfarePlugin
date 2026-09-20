package ru.dronewarfare;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.minimessage.MiniMessage;

public final class DroneWarfarePlugin extends JavaPlugin implements CommandExecutor, Listener {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private static final double ARRIVAL_DISTANCE = 2.0;
    private static final double FLIGHT_SPEED = 0.5;
    private static final float EXPLOSION_POWER = 8.0F;

    @Override
    public void onEnable() {
        if (getCommand("drone") != null) {
            getCommand("drone").setExecutor(this);
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("DroneWarfarePlugin enabled.");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    miniMessage.deserialize(
                            "<red>Команду может использовать только игрок.</red>"
                    )
            );
            return true;
        }

        if (args.length != 4
                || !args[0].equalsIgnoreCase("launch")) {

            player.sendMessage(
                    miniMessage.deserialize(
                            "<red>Использование: /drone launch <x> <y> <z></red>"
                    )
            );

            return true;
        }

        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);

            Location target = new Location(
                    player.getWorld(),
                    x,
                    y,
                    z
            );

            Location spawn =
                    player.getLocation()
                            .clone()
                            .add(0, 2, 0);

            spawnDrone(
                    spawn,
                    target
            );

            player.sendMessage(
                    miniMessage.deserialize(
                            "<green>Реактивный дрон запущен к "
                                    + x
                                    + ", "
                                    + y
                                    + ", "
                                    + z
                                    + "</green>"
                    )
            );

        } catch (NumberFormatException e) {
            player.sendMessage(
                    miniMessage.deserialize(
                            "<red>Координаты должны быть числами.</red>"
                    )
            );
        }

        return true;
    }

    private void spawnDrone(
            Location spawnLocation,
            Location targetLocation
    ) {
        Bukkit.getRegionScheduler().execute(
                this,
                spawnLocation,
                () -> {

                    Phantom phantom =
                            (Phantom) spawnLocation.getWorld()
                                    .spawnEntity(
                                            spawnLocation,
                                            EntityType.PHANTOM
                                    );

                    /*
                     * Дрон можно сбить.
                     */
                    phantom.setInvulnerable(false);

                    /*
                     * Немного здоровья,
                     * чтобы обычного попадания было достаточно.
                     */
                    phantom.setHealth(8.0);

                    /*
                     * Дрон не горит от солнца.
                     */
                    phantom.setFireTicks(0);

                    startFlight(
                            phantom,
                            targetLocation
                    );
                }
        );
    }

    private void startFlight(
            Phantom phantom,
            Location targetLocation
    ) {
        phantom.getScheduler().runAtFixedRate(
                this,

                task -> {

                    if (!phantom.isValid()) {
                        task.cancel();
                        return;
                    }

                    Location current =
                            phantom.getLocation();

                    if (current.getWorld() == null
                            || targetLocation.getWorld() == null
                            || !current.getWorld().equals(
                                    targetLocation.getWorld()
                            )) {

                        phantom.remove();
                        task.cancel();
                        return;
                    }

                    double distance =
                            current.distance(targetLocation);

                    /*
                     * Дрон достиг цели.
                     */
                    if (distance <= ARRIVAL_DISTANCE) {

                        Location explosionLocation =
                                phantom.getLocation().clone();

                        phantom.remove();
                        task.cancel();

                        Bukkit.getRegionScheduler().execute(
                                this,
                                explosionLocation,
                                () -> explosionLocation.getWorld()
                                        .createExplosion(
                                                explosionLocation,
                                                EXPLOSION_POWER,
                                                true,
                                                true
                                        )
                        );

                        return;
                    }

                    /*
                     * Направление на цель.
                     */
                    Vector direction =
                            targetLocation.toVector()
                                    .subtract(
                                            current.toVector()
                                    )
                                    .normalize();

                    /*
                     * Поворачиваем Phantom
                     * в направлении движения.
                     */
                    float yaw =
                            (float) Math.toDegrees(
                                    Math.atan2(
                                            -direction.getX(),
                                            direction.getZ()
                                    )
                            );

                    float pitch =
                            (float) Math.toDegrees(
                                    Math.asin(
                                            -direction.getY()
                                    )
                            );

                    phantom.setRotation(
                            yaw,
                            pitch
                    );

                    /*
                     * Полёт.
                     */
                    phantom.setVelocity(
                            direction.multiply(
                                    FLIGHT_SPEED
                            )
                    );

                    /*
                     * Визуал и звук реактивного двигателя.
                     */
                    playFlightEffects(
                            phantom,
                            direction
                    );
                },

                () -> {},

                1L,
                1L
        );
    }

    private void playFlightEffects(
            Phantom phantom,
            Vector direction
    ) {
        Location location =
                phantom.getLocation();

        World world =
                phantom.getWorld();

        /*
         * Точка выхода двигателя находится
         * позади Phantom относительно направления полёта.
         */
        Vector backwards =
                direction.clone()
                        .multiply(-1);

        Location engine =
                location.clone()
                        .add(
                                backwards.multiply(1.3)
                        );

        /*
         * Огненный реактивный след.
         */
        world.spawnParticle(
                Particle.FLAME,
                engine,
                3,
                0.12,
                0.12,
                0.12,
                0.02
        );

        /*
         * Горячий воздух / дым.
         */
        world.spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                engine,
                1,
                0.08,
                0.08,
                0.08,
                0.01
        );

        /*
         * Искры.
         */
        world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                engine,
                1,
                0.1,
                0.1,
                0.1,
                0.02
        );

        /*
         * Редкий звук двигателя.
         * Не играем слишком громко каждый тик.
         */
        if (phantom.getTicksLived() % 6 == 0) {
            world.playSound(
                    location,
                    Sound.ENTITY_BLAZE_SHOOT,
                    0.25f,
                    1.7f
            );
        }

        /*
         * Ещё один более низкий звук,
         * создающий ощущение пролёта.
         */
        if (phantom.getTicksLived() % 20 == 0) {
            world.playSound(
                    location,
                    Sound.ENTITY_PHANTOM_FLAP,
                    0.5f,
                    0.6f
            );
        }
    }

    @EventHandler
    public void onDroneDamage(
            EntityDamageByEntityEvent event
    ) {
        if (!(event.getEntity() instanceof Phantom phantom)) {
            return;
        }

        /*
         * Если Phantom получил урон,
         * он остаётся обычным сбиваемым мобом.
         */
        phantom.setInvulnerable(false);
    }

    @EventHandler
    public void onDroneDeath(
            EntityDeathEvent event
    ) {
        if (!(event.getEntity() instanceof Phantom)) {
            return;
        }

        /*
         * Уничтоженный стрелой/арбалетом Phantom
         * просто падает и НЕ взрывается.
         */
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
}