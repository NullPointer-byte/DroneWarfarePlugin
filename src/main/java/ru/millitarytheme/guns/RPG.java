package ru.millitarytheme.guns;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.millitarytheme.ExplosionUtil;
import ru.millitarytheme.MillitaryTheme;

public class RPG implements Listener {

    private static final int RADIUS = 10;
    private static final String RPG_TAG = "#RPG";

    private final NamespacedKey rpgKey;
    private final NamespacedKey modelKey;
    private final MillitaryTheme plugin;

    public RPG(MillitaryTheme plugin) {
        this.plugin = plugin;
        this.rpgKey = new NamespacedKey(plugin, "rpg_weapon");
        this.modelKey = new NamespacedKey("militarytheme", "rpg_model");
    }

    public void giveRPG(Player player) {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = crossbow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cRPG");
            meta.setItemModel(modelKey); // ← кастомная модель
            meta.getPersistentDataContainer().set(rpgKey, PersistentDataType.BYTE, (byte) 1);
            crossbow.setItemMeta(meta);
        }
        player.getInventory().addItem(crossbow);
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType() != Material.CROSSBOW) return;
        ItemMeta meta = bow.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(rpgKey, PersistentDataType.BYTE)) return;

        Entity projectile = event.getProjectile();
        if (projectile != null) {
            projectile.getPersistentDataContainer()
                    .set(rpgKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!projectile.getPersistentDataContainer().has(rpgKey, PersistentDataType.BYTE)) return;

        Location hitLoc = projectile.getLocation();
        if (hitLoc.getWorld() == null) return;

        hitLoc.getWorld().createExplosion(hitLoc, 4.0f, false, false);

        ExplosionUtil.makeCrater(plugin, hitLoc, RADIUS, 0, RPG_TAG);

        projectile.remove();
    }
}