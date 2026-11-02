package ru.millitarytheme.guns;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import ru.millitarytheme.MillitaryTheme;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Flamethrower implements Listener {

    private static final double RANGE = 5.0;
    private static final double CONE = 0.85;
    private static final double DAMAGE = 1.5;
    private static final long FIRE_INTERVAL = 2L;
    private static final String LOG_TAG = "#Flamethrower";

    private final NamespacedKey flameKey;
    private final NamespacedKey modelKey;
    private final MillitaryTheme plugin;
    private final Set<UUID> firing = new HashSet<>();

    public Flamethrower(MillitaryTheme plugin) {
        this.plugin = plugin;
        this.flameKey = new NamespacedKey(plugin, "flamethrower_weapon");
        this.modelKey = new NamespacedKey("militarytheme", "flamethrower_model");
    }

    public void giveFlamethrower(Player player) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Огнемёт");
            meta.setItemModel(modelKey);
            meta.getPersistentDataContainer().set(flameKey, PersistentDataType.BYTE, (byte) 1);
            bow.setItemMeta(meta);
        }
        player.getInventory().addItem(bow);
    }

    private boolean isFlamethrower(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(flameKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isFlamethrower(player.getInventory().getItemInMainHand())) return;

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

        // Folia
        player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()
                    || !firing.contains(id)
                    || !isFlamethrower(player.getInventory().getItemInMainHand())) {
                firing.remove(id);
                task.cancel();
                return;
            }
            spitFire(player);
        }, null, 1L, FIRE_INTERVAL);
    }

    private void stopFiring(Player player) {
        firing.remove(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        firing.remove(event.getPlayer().getUniqueId());
    }

    private void spitFire(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        // Партиклы
        for (double d = 0.5; d <= RANGE; d += 0.4) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.FLAME, point, 4, 0.12, 0.12, 0.12, 0.01);
            world.spawnParticle(Particle.SMOKE, point, 1, 0.08, 0.08, 0.08, 0.01);
        }

        // Ставим огонь по лучу (только в воздух)
        for (double d = 0.8; d <= RANGE; d += 0.6) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            Block block = point.getBlock();

            if (!block.getType().isAir() && block.getType() != Material.FIRE) continue;
            if (block.getType() == Material.FIRE) continue;

            BlockData oldData = block.getBlockData();
            block.setType(Material.FIRE);

            try {
                plugin.getRollback().addLog(
                        LOG_TAG,
                        world.getName(),
                        block.getX(),
                        block.getY(),
                        block.getZ(),
                        oldData.getAsString(),
                        "minecraft:fire"
                );
            } catch (Exception e) {
                plugin.getLogger().warning("Rollback log failed: " + e.getMessage());
            }
        }

        // Урон по существам в конусе
        for (org.bukkit.entity.Entity entity : world.getNearbyEntities(eye, RANGE, RANGE, RANGE)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity living)) continue;

            Vector toEntity = entity.getLocation().toVector().subtract(eye.toVector());
            double distSq = toEntity.lengthSquared();
            if (distSq > RANGE * RANGE || distSq < 0.01) continue;

            if (toEntity.normalize().dot(dir) < CONE) continue;

            living.damage(DAMAGE, player);
            living.setFireTicks(Math.max(living.getFireTicks(), 80));
        }
    }
}