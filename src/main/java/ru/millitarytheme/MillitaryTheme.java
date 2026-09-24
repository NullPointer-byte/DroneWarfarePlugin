package ru.millitarytheme;

import org.bukkit.plugin.java.JavaPlugin;

import ru.millitarytheme.command.DroneCommand;
import ru.millitarytheme.drone.DroneManager;
import ru.millitarytheme.listener.DroneListener;

public final class MillitaryTheme extends JavaPlugin {

    private DroneManager droneManager;

    @Override
    public void onEnable() {
        droneManager = new DroneManager(this);

        if (getCommand("drone") != null) {
            getCommand("drone").setExecutor(
                    new DroneCommand(droneManager, this)
            );
        }

        getServer().getPluginManager().registerEvents(
                new DroneListener(droneManager),
                this
        );

        getLogger().info("MillitaryTheme enabled.");
    }

    @Override
    public void onDisable() {
        droneManager.shutdown();

        getLogger().info("MillitaryTheme disabled.");
    }

    public DroneManager getDroneManager() {
        return droneManager;
    }
}