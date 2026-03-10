package pl.Dowimixworsafe.reviveRitual.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;

public class GhostListener implements Listener {

    private final ReviveRitual plugin;

    public GhostListener(ReviveRitual plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity target = event.getRightClicked();

        if (target instanceof org.bukkit.entity.Interaction) {
            plugin.getTombstoneManager().tryLootGrave(event.getPlayer(), (org.bukkit.entity.Interaction) target);
            return;
        }

        if (target instanceof Player && plugin.getDataManager().isDead((Player) target)) {
            event.setCancelled(true);
        }
        if (plugin.getGhostManager().ghostEntities.containsValue(target.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        if (plugin.getGhostManager().ghostEntities.containsValue(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGhostReceiveDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && plugin.getDataManager().isDead((Player) e.getEntity())) {
            e.setCancelled(true);
        }
        if (plugin.getGhostManager().ghostEntities.containsValue(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player && plugin.getDataManager().isDead((Player) event.getTarget())) {
            event.setCancelled(true);
        }
        if (event.getTarget() != null
                && plugin.getGhostManager().ghostEntities.containsValue(event.getTarget().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGhostInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDataManager().isDead(player)) {
            String mode = plugin.getConfig().getString("punishment-mode");
            if (mode.equalsIgnoreCase("ghost")) {
                if (event.getItem() != null && event.getItem().getType() == Material.POLISHED_BLACKSTONE_BUTTON) {
                    if (event.getAction().name().contains("RIGHT")) {
                        plugin.getPunishmentManager().handleHaunt(player);
                        player.getInventory().setHeldItemSlot(0);
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onGhostPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player && plugin.getDataManager().isDead((Player) e.getEntity()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onGhostDrop(PlayerDropItemEvent e) {
        if (plugin.getDataManager().isDead(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player && plugin.getDataManager().isDead((Player) e.getWhoClicked()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent e) {
        if (plugin.getDataManager().isDead(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onGhostBreak(BlockBreakEvent e) {
        if (plugin.getDataManager().isDead(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onGhostPlace(BlockPlaceEvent e) {
        if (plugin.getDataManager().isDead(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onGhostDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && plugin.getDataManager().isDead((Player) e.getDamager()))
            e.setCancelled(true);
    }
}