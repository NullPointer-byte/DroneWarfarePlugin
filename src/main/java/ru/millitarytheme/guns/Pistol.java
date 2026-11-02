package ru.millitarytheme.guns;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Pistol implements Listener {

    private static final double BULLET_SPEED = 3.5;
    private static final double DAMAGE = 6.0;

    private final NamespacedKey pistolKey;
    private final NamespacedKey modelKey;
    private final JavaPlugin plugin;

    public Pistol(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pistolKey = new NamespacedKey(plugin, "pistol_weapon");
        this.modelKey = new NamespacedKey("militarytheme", "pistol_model");
    }

    public void givePistol(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eПистолет");
            meta.setItemModel(modelKey); // ← кастомная модель
            meta.getPersistentDataContainer().set(pistolKey, PersistentDataType.BYTE, (byte) 1);
            bow.setItemMeta(meta);
        }
        player.getInventory().addItem(bow);
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;
        ItemMeta meta = bow.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(pistolKey, PersistentDataType.BYTE)) return;

        event.setCancelled(true);

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Location spawn = eye.clone().add(dir.clone().multiply(1.0));

        Arrow bullet = player.getWorld().spawnArrow(
                spawn,
                dir,
                (float) BULLET_SPEED,
                0f
        );
        bullet.setGravity(false);
        bullet.setShooter(player);
        bullet.setDamage(DAMAGE);
        bullet.setPickupStatus(Arrow.PickupStatus.DISALLOWED);

        bullet.getPersistentDataContainer()
                .set(pistolKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(pistolKey, PersistentDataType.BYTE)) return;

        event.setDamage(DAMAGE);
        arrow.remove();
    }
}