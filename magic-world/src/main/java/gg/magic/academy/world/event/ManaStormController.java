package gg.magic.academy.world.event;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * Schedules and runs Mana Storm events — chaotic magic surges in the open world.
 * During a storm: enhanced mob spawns, high-tier loot drops, visual effects.
 */
public class ManaStormController {

    private final JavaPlugin plugin;
    private final Random rng = new Random();
    private boolean stormActive = false;

    // Config values (could be moved to YAML)
    private static final long MIN_INTERVAL_TICKS = 72000L;  // 1 hour
    private static final long MAX_INTERVAL_TICKS = 144000L; // 2 hours
    private static final long STORM_DURATION_TICKS = 6000L; // 5 minutes

    public ManaStormController(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule() {
        long delay = MIN_INTERVAL_TICKS + (long)(rng.nextDouble() * (MAX_INTERVAL_TICKS - MIN_INTERVAL_TICKS));
        plugin.getServer().getScheduler().runTaskLater(plugin, this::startStorm, delay);
    }

    private void startStorm() {
        if (stormActive) {
            schedule();
            return;
        }
        stormActive = true;

        plugin.getServer().broadcast(
                Component.text("⚡ A Mana Storm surges across the world! Rare drops await brave mages!")
                        .color(TextColor.color(0xAA00FF))
        );

        // Apply storm effects to the main world
        World world = plugin.getServer().getWorld("world");
        if (world != null) {
            world.setStorm(true);
            world.setThundering(true);
        }

        // Spawn extra mobs in world zones (placeholder — real impl uses zone mob tables)
        // TODO: integrate with ZoneManager to spawn from zone mob_table

        // End storm after duration
        plugin.getServer().getScheduler().runTaskLater(plugin, this::endStorm, STORM_DURATION_TICKS);
    }

    private void endStorm() {
        stormActive = false;
        plugin.getServer().broadcast(
                Component.text("⚡ The Mana Storm subsides.")
                        .color(TextColor.color(0x8855FF))
        );
        World world = plugin.getServer().getWorld("world");
        if (world != null) {
            world.setStorm(false);
            world.setThundering(false);
        }
        schedule(); // schedule next storm
    }

    public boolean isStormActive() { return stormActive; }
}
