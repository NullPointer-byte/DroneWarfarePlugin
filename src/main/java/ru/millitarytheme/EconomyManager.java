package ru.millitarytheme;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private static Economy econ = null;

    public static boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        } else {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            } else {
                econ = (Economy) rsp.getProvider();
                return econ != null;
            }
        }
    }

    public static boolean canAfford(Player p, double amount) {
        return econ == null ? true : econ.has(p, amount);
    }

    public static boolean withdraw(Player p, double amount) {
        if (econ == null) {
            return true;
        } else if (econ.has(p, amount)) {
            econ.withdrawPlayer(p, amount);
            return true;
        } else {
            return false;
        }
    }

    public static double getBalance(Player p) {
        return econ == null ? (double) 0.0F : econ.getBalance(p);
    }
}