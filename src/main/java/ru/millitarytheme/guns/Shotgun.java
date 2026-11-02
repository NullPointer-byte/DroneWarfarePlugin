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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Shotgun implements Listener {

    private static final int PELLETS = 5;
    private static final double BULLET_SPEED = 4.0;
    private static final double DAMAGE = 2.5;
    private static final double SPREAD = 0.15;
    private static final int COOLDOWN_TICKS = 30;

    private final NamespacedKey shotgunKey;
    private final NamespacedKey modelKey;
    private final JavaPlugin plugin;

    public Shotgun(JavaPlugin plugin) {
        this.plugin = plugin;
        this.shotgunKey = new NamespacedKey(plugin, "shotgun_weapon");
        this.modelKey = new NamespacedKey("militarytheme", "shotgun_model");
    }

    public void giveShotgun(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cДробовик");
            meta.setItemModel(modelKey); // ← кастомная модель
            meta.getPersistentDataContainer().set(shotgunKey, PersistentDataType.BYTE, (byte) 1);
            bow.setItemMeta(meta);
        }
        player.getInventory().addItem(bow);
    }

    private boolean isShotgun(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(shotgunKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isShotgun(item)) return;

        event.setCancelled(true);

        if (player.hasCooldown(item.getType())) return;

        shoot(player);
        player.setCooldown(item.getType(), COOLDOWN_TICKS);
    }

    private void shoot(Player player) {
        Location eye = player.getEyeLocation();
        Vector baseDir = eye.getDirection().normalize();

        for (int i = 0; i < PELLETS; i++) {
            Vector dir = baseDir.clone().add(new Vector(
                    (Math.random() - 0.5) * SPREAD,
                    (Math.random() - 0.5) * SPREAD,
                    (Math.random() - 0.5) * SPREAD
            )).normalize();

            Location spawn = eye.clone().add(dir.clone().multiply(1.0));

            Arrow pellet = player.getWorld().spawnArrow(
                    spawn, dir, (float) BULLET_SPEED, 0f
            );
            pellet.setGravity(false);
            pellet.setShooter(player);
            pellet.setDamage(DAMAGE);
            pellet.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            pellet.getPersistentDataContainer()
                    .set(shotgunKey, PersistentDataType.BYTE, (byte) 1);
        }

        player.getWorld().playSound(
                player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.4f
        );
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getBow() != null && isShotgun(event.getBow())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(shotgunKey, PersistentDataType.BYTE)) return;
        event.setDamage(DAMAGE);
        arrow.remove();
    }
}