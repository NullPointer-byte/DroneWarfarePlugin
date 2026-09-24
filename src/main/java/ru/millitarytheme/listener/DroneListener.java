package ru.millitarytheme.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Phantom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.millitarytheme.drone.Drone;
import ru.millitarytheme.drone.DroneManager;

public final class DroneListener implements Listener {

    private final DroneManager droneManager;

    public DroneListener(DroneManager droneManager) {
        this.droneManager = droneManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        Entity entity =
                event.getEntity();

        if (!(entity instanceof Phantom phantom)) {
            return;
        }

        for (Drone drone : droneManagerSnapshot()) {

            if (drone.getPhantom() != phantom) {
                continue;
            }

            droneManager.remove(
                    drone.getPilot()
            );

            event.getDrops().clear();
            event.setDroppedExp(0);

            return;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        droneManager.remove(
                event.getPlayer()
        );
    }

    private Iterable<Drone> droneManagerSnapshot() {
        return java.util.List.copyOf(
                droneManager.getDrones()
        );
    }
}