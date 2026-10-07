package ru.millitarytheme;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import ru.millitarytheme.command.DroneCommand;
import ru.millitarytheme.drone.DroneManager;
import ru.millitarytheme.listener.DroneListener;
import ru.florestdev.florestRollback.FlorestRollback;

public final class MillitaryTheme extends JavaPlugin {

    private DroneManager droneManager;
    private FlorestRollback rollback;

    public FlorestRollback getRollback() {
        return  rollback;
    }

    @Override
    public void onEnable() {
        droneManager = new DroneManager(this);

        EconomyManager.setupEconomy();

        if (getCommand("drone") != null) {
            getCommand("drone").setExecutor(
                    new DroneCommand(droneManager, this)
            );
        }

        getServer().getPluginManager().registerEvents(
                new DroneListener(droneManager),
                this
        );

        Plugin getter = this.getServer().getPluginManager().getPlugin("FlorestRollback");
        if (getter != null) {
            rollback = (FlorestRollback) getter;
        } else {
            getLogger().severe("FlorestRollback is not responding!");
            getServer().getPluginManager().disablePlugin(this);
        }

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