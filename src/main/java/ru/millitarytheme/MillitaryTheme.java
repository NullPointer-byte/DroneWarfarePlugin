package ru.millitarytheme;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import ru.millitarytheme.command.DroneCommand;
import ru.millitarytheme.command.GunCommand;
import ru.millitarytheme.drone.DroneManager;
import ru.millitarytheme.guns.*;
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

        if (getCommand("gun") != null) {
            getCommand("gun").setExecutor(new GunCommand(this));
        }

        getServer().getPluginManager().registerEvents(
                new DroneListener(droneManager),
                this
        );

        getServer().getPluginManager().registerEvents(new InstallRPOnConnect(), this);

        Plugin getter = this.getServer().getPluginManager().getPlugin("FlorestRollback");
        if (getter != null) {
            rollback = (FlorestRollback) getter;
        } else {
            getLogger().severe("FlorestRollback is not responding!");
            getServer().getPluginManager().disablePlugin(this);
        }

        // GUNS
        getServer().getPluginManager().registerEvents(new RPG(this), this);
        getServer().getPluginManager().registerEvents(new Pistol(this), this);
        getServer().getPluginManager().registerEvents(new AK(this), this);
        getServer().getPluginManager().registerEvents(new Shotgun(this), this);
        getServer().getPluginManager().registerEvents(new Sniper(this), this);
        getServer().getPluginManager().registerEvents(new Flamethrower(this), this);

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