package gg.magic.academy.dungeons.instance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class DungeonBossDeathListener implements Listener {

    private final DungeonInstanceManager instanceManager;

    public DungeonBossDeathListener(DungeonInstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        instanceManager.handleBossDeath(event.getEntity().getUniqueId());
    }
}
