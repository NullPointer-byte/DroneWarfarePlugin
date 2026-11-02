package ru.millitarytheme.guns;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
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

public class Sniper implements Listener {

    private static final double BULLET_SPEED = 10.0;
    private static final double DAMAGE = 9.0;
    private static final int COOLDOWN_TICKS = 45; // ~2.25 сек

    private final NamespacedKey sniperKey;
    private final NamespacedKey modelKey;
    private final JavaPlugin plugin;

    public Sniper(JavaPlugin plugin) {
        this.plugin = plugin;
        this.sniperKey = new NamespacedKey(plugin, "sniper_weapon");
        this.modelKey = new NamespacedKey("militarytheme", "sniper_model");
    }

    public void giveSniper(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bСнайперка");
            meta.setItemModel(modelKey);
            meta.getPersistentDataContainer().set(sniperKey, PersistentDataType.BYTE, (byte) 1);
            bow.setItemMeta(meta);
        }
        player.getInventory().addItem(bow);
    }

    private boolean isSniper(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(sniperKey, PersistentDataType.BYTE);
    }

    // Теперь можно нормально целиться — стреляем только при отпускании тетивы
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !isSniper(bow)) return;

        // Отменяем ванильную стрелу
        event.setCancelled(true);

        // Кулдаун
        if (player.hasCooldown(Material.BOW)) return;
        player.setCooldown(Material.BOW, COOLDOWN_TICKS);

        // Своя быстрая пуля
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Location spawn = eye.clone().add(dir.clone().multiply(1.4));

        Arrow bullet = player.getWorld().spawnArrow(spawn, dir, (float) BULLET_SPEED, 0f);
        bullet.setGravity(false);
        bullet.setShooter(player);
        bullet.setDamage(DAMAGE);
        bullet.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        bullet.getPersistentDataContainer().set(sniperKey, PersistentDataType.BYTE, (byte) 1);

        player.getWorld().playSound(
                player.getLocation(),
                org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.55f, 1.85f
        );
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(sniperKey, PersistentDataType.BYTE)) return;
        event.setDamage(DAMAGE);
        arrow.remove();
    }
}