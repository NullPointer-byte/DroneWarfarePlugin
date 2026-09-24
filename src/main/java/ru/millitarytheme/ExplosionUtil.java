package ru.millitarytheme;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import ru.millitarytheme.MillitaryTheme;

public final class ExplosionUtil {

    private ExplosionUtil() {}

    /**
     * Ломает сферу блоков и логирует каждый в rollback.
     *
     * @param tag   тег для rollback (например "#Drone", "#FpvDrone")
     * @param radius радиус сферы
     * @param maxY  ограничение сверху (0 = без ограничения)
     */
    public static void makeCrater(
            MillitaryTheme plugin,
            Location center,
            int radius,
            int maxY,
            String tag
    ) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int r2 = radius * radius;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {

                if (maxY > 0 && y > maxY) continue;

                for (int z = -radius; z <= radius; z++) {

                    if (x * x + y * y + z * z > r2) continue;

                    Block block = world.getBlockAt(
                            cx + x, cy + y, cz + z
                    );

                    // Не трогаем воздух и бедрок.
                    if (block.getType().isAir()) continue;
                    if (block.getType() == org.bukkit.Material.BEDROCK) continue;

                    // ВАЖНО: снимок данных ДО поломки.
                    BlockData oldData = block.getBlockData();

                    block.breakNaturally();

                    try {
                        plugin.getRollback().addLog(
                                tag,
                                world.getName(),
                                cx + x,
                                cy + y,
                                cz + z,
                                oldData.getAsString(),
                                "minecraft:air"
                        );
                    } catch (Exception e) {
                        plugin.getLogger().warning(
                                "Rollback log failed: " + e.getMessage()
                        );
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}