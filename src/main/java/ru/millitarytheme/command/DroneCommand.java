package ru.millitarytheme.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.millitarytheme.MillitaryTheme;
import ru.millitarytheme.drone.Drone;
import ru.millitarytheme.drone.DroneManager;
import ru.millitarytheme.drone.FpvDrone;
import ru.millitarytheme.drone.MQ1PredatorDrone;

public final class DroneCommand implements CommandExecutor {

    private final DroneManager droneManager;
    private final MillitaryTheme plugin;

    public DroneCommand(
            DroneManager droneManager,
            MillitaryTheme plugin
    ) {
        this.droneManager = droneManager;
        this.plugin = plugin;
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
                    "Команду может использовать только игрок."
            );
            return true;
        }

        if (args.length != 1) {
            sendUsage(player);
            return true;
        }

        if (droneManager.hasDrone(player)) {
            player.sendMessage(
                    "У тебя уже есть активный дрон."
            );
            return true;
        }

        /*
         * Shahed-дрон.
         */
        if (args[0].equalsIgnoreCase("shahed")) {

            Drone drone = droneManager.launch(player);

            if (drone == null) {
                player.sendMessage(
                        "Не удалось запустить Shahed."
                );
                return true;
            }

            player.sendMessage(
                    "Shahed запущен."
            );

            return true;
        }

        /*
         * FPV-дрон.
         */
        if (args[0].equalsIgnoreCase("fpv")) {

            FpvDrone drone = droneManager.launchFpv(player);

            if (drone == null) {
                player.sendMessage(
                        "Не удалось запустить FPV-дрон."
                );
                return true;
            }

            player.sendMessage(
                    "FPV-дрон запущен."
            );

            return true;
        }

        /*
         * MQ-1 Predator.
         */
        if (args[0].equalsIgnoreCase("predator")) {

            Location location = player.getLocation()
                    .clone()
                    .add(0, 5, 0);

            new MQ1PredatorDrone(
                    player,
                    location,
                    plugin
            );

            player.sendMessage(
                    "MQ-1 Predator запущен."
            );

            return true;
        }

        sendUsage(player);
        return true;
    }

    private void sendUsage(Player player) {

        player.sendMessage(
                "Использование:"
        );

        player.sendMessage(
                "/drone shahed"
        );

        player.sendMessage(
                "/drone fpv"
        );

        player.sendMessage(
                "/drone predator"
        );
    }
}