package ru.millitarytheme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import ru.millitarytheme.drone.MQ1PredatorDrone;

public final class PredatorListener implements Listener {

    private final MQ1PredatorDrone predator;

    public PredatorListener(MQ1PredatorDrone predator) {
        this.predator = predator;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (!event.getPlayer().equals(predator.getPilot())) {
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_AIR
                && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        predator.handleLeftClick();
    }
}