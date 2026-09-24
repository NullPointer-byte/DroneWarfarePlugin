package ru.millitarytheme.drone;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DroneManager {

    private final JavaPlugin plugin;

    private final Map<UUID, Drone> drones = new HashMap<>();

    private final Map<UUID, FpvDrone> fpvDrones = new HashMap<>();

    public DroneManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Drone launch(Player player) {

        if (hasDrone(player)) {
            return null;
        }

        Drone drone = new Drone(
                plugin,
                this,
                player
        );

        drones.put(
                player.getUniqueId(),
                drone
        );

        drone.spawn();

        return drone;
    }

    public FpvDrone launchFpv(Player player) {

    if (hasDrone(player)) {
        return null;
    }

    FpvDrone drone =
            new FpvDrone(
                    plugin,
                    this,
                    player
            );

    fpvDrones.put(
            player.getUniqueId(),
            drone
    );

    drone.spawn();

    return drone;
}

    public void remove(Player player) {
        remove(player.getUniqueId());
    }

    public void remove(UUID playerId) {

        Drone drone =
                drones.remove(playerId);

        if (drone != null) {
            drone.destroy();
        }
    }

    /*
     * Удаляет дрон из менеджера,
     * но не вызывает destroy() повторно.
     */
    public void unregister(Player player) {

    UUID uuid =
            player.getUniqueId();

    drones.remove(uuid);
    fpvDrones.remove(uuid);
}

    public void unregister(UUID playerId) {
        drones.remove(playerId);
    }

    public Drone getDrone(Player player) {
        return drones.get(
                player.getUniqueId()
        );
    }

    public boolean hasDrone(Player player) {

    UUID uuid =
            player.getUniqueId();

    return drones.containsKey(uuid)
            || fpvDrones.containsKey(uuid);
}

    public Collection<Drone> getDrones() {
        return drones.values();
    }

    public void shutdown() {

        for (Drone drone :
                drones.values()) {

            drone.destroy();
        }

        drones.clear();
    }
}