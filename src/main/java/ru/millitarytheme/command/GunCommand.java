package ru.millitarytheme.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.millitarytheme.EconomyManager;
import ru.millitarytheme.MillitaryTheme;
import ru.millitarytheme.guns.AK;
import ru.millitarytheme.guns.Flamethrower;
import ru.millitarytheme.guns.Pistol;
import ru.millitarytheme.guns.RPG;
import ru.millitarytheme.guns.Shotgun;
import ru.millitarytheme.guns.Sniper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GunCommand implements CommandExecutor, TabCompleter {

    /** Название (нижний регистр) -> цена. */
    private static final Map<String, Double> PRICES = new LinkedHashMap<>();

    static {
        PRICES.put("pistol", 1500.0);
        PRICES.put("shotgun", 4000.0);
        PRICES.put("ak", 7000.0);
        PRICES.put("sniper", 9000.0);
        PRICES.put("rpg", 12000.0);
        PRICES.put("flamethrower", 15000.0);
    }

    private final MillitaryTheme plugin;

    public GunCommand(MillitaryTheme plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Консоли запрещено.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eИспользование: /" + label + " <оружие>");
            player.sendMessage("§7Доступно: " + String.join(", ", PRICES.keySet()));
            return true;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        Double price = PRICES.get(name);

        if (price == null) {
            player.sendMessage("§cНеизвестное оружие: " + args[0]);
            player.sendMessage("§7Доступно: " + String.join(", ", PRICES.keySet()));
            return true;
        }

        if (!EconomyManager.canAfford(player, price)) {
            player.sendMessage("§cНедостаточно средств. Нужно: §e" + price
                    + "§c, у вас: §e" + EconomyManager.getBalance(player));
            return true;
        }

        if (!EconomyManager.withdraw(player, price)) {
            player.sendMessage("§cНе удалось списать деньги.");
            return true;
        }

        giveWeapon(player, name);

        player.sendMessage("§aКуплено: §e" + name + " §aза §e" + price);
        return true;
    }

    private void giveWeapon(Player player, String name) {
        switch (name) {
            case "pistol" -> new Pistol(plugin).givePistol(player);
            case "shotgun" -> new Shotgun(plugin).giveShotgun(player);
            case "ak" -> new AK(plugin).giveAK(player);
            case "sniper" -> new Sniper(plugin).giveSniper(player);
            case "rpg" -> new RPG(plugin).giveRPG(player);
            case "flamethrower" -> new Flamethrower(plugin).giveFlamethrower(player);
            default -> player.sendMessage("§cОшибка: оружие не реализовано.");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String @NotNull [] args) {
        if (args.length != 1) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();

        for (String key : PRICES.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key);
            }
        }
        return result;
    }
}