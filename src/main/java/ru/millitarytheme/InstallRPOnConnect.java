package ru.millitarytheme;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.geyser.api.GeyserApi;


// Installing Resource-Pack on connect.
public class InstallRPOnConnect implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GeyserApi api = GeyserApi.api();
        if (api == null) {
            player.setResourcePack("https://raw.githubusercontent.com/florestdev/DroneWarfarePlugin/florest_code/datapack/MilitaryThemeGuns.zip");
            return;
        }
        if (!api.isBedrockPlayer(player.getUniqueId())) {
            player.setResourcePack("https://raw.githubusercontent.com/florestdev/DroneWarfarePlugin/florest_code/datapack/MilitaryThemeGuns.zip");
        }
    }
}
