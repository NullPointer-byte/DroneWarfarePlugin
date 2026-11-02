package ru.millitarytheme.guns;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AK implements Listener {

    private static final double BULLET_SPEED = 4.5;
    private static final double DAMAGE = 4.0;
    private static final long FIRE_INTERVAL = 4L;
    private static final double SPREAD = 0.02;

    private final NamespacedKey akKey;
    private final NamespacedKey modelKey;
    private final JavaPlugin plugin;
    private final Set<UUID> firing = new HashSet<>();

    public AK(JavaPlugin plugin) {
        this.plugin = plugin;
        this.akKey = new NamespacedKey(plugin, "ak_weapon");
        this.modelKey = new NamespacedKey("militarytheme", "ak_model");
    }

    public void giveAK(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6АК");
            meta.setItemModel(modelKey);
            meta.getPersistentDataContainer().set(akKey, PersistentDataType.BYTE, (byte) 1);
            bow.setItemMeta(meta);
        }
        player.getInventory().addItem(bow);
    }

    private boolean isAK(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(akKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isAK(player.getInventory().getItemInMainHand())) return;

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            startFiring(player);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            stopFiring(player);
        }
    }

    private void startFiring(Player player) {
        UUID id = player.getUniqueId();
        if (!firing.add(id)) return;

        // Folia-совместимый планировщик
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()
                    || !firing.contains(id)
                    || !isAK(player.getInventory().getItemInMainHand())) {
                firing.remove(id);
                task.cancel();
                return;
            }
            shoot(player);
        }, null, 1L, FIRE_INTERVAL);
    }

    private void stopFiring(Player player) {
        firing.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        firing.remove(event.getPlayer().getUniqueId());
    }

    private void shoot(Player player) {
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        dir.add(new Vector(
                (Math.random() - 0.5) * SPREAD,
                (Math.random() - 0.5) * SPREAD,
                (Math.random() - 0.5) * SPREAD
        )).normalize();

        Location spawn = eye.clone().add(dir.clone().multiply(1.2));

        Arrow bullet = player.getWorld().spawnArrow(spawn, dir, (float) BULLET_SPEED, 0f);
        bullet.setGravity(false);
        bullet.setShooter(player);
        bullet.setDamage(DAMAGE);
        bullet.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        bullet.getPersistentDataContainer().set(akKey, PersistentDataType.BYTE, (byte) 1);

        player.getWorld().playSound(
                player.getLocation(),
                org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                0.6f, 1.6f
        );
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getBow() != null && isAK(event.getBow())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(akKey, PersistentDataType.BYTE)) return;
        event.setDamage(DAMAGE);
        arrow.remove();
    }
}