package pl.Dowimixworsafe.reviveRitual.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;

import java.util.Collection;
import java.util.UUID;

public class CauldronListener implements Listener {

    private final ReviveRitual plugin;
    private final String BLOOD_TAG = "custom_blood_liquid";
    private final String TARGET_PREFIX = "target_player_";

    public CauldronListener(ReviveRitual plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getDataManager().isDead(event.getPlayer())) return;

        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.hasBlock()) return;

        Block block = event.getClickedBlock();
        if (block.getType() != Material.CAULDRON && block.getType() != Material.WATER_CAULDRON) return;

        ItemStack item = event.getItem();
        TextDisplay bloodEntity = findBloodEntity(block);

        if (item != null && item.getType() == Material.POTION && bloodEntity == null) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null && meta.getBasePotionType() == PotionType.STRONG_HEALING) {
                if (!meta.hasDisplayName()) {
                    event.getPlayer().sendMessage(plugin.getConfigManager().getMsg("potion-no-name"));
                    return;
                }
                String targetName = ChatColor.stripColor(meta.getDisplayName());
                UUID targetUUID = findPlayerUUID(targetName);

                if (targetUUID == null || !plugin.getDataManager().getData().getString("status." + targetUUID, "alive").equals("dead")) {
                    event.getPlayer().sendMessage(plugin.getConfigManager().getMsg("player-not-dead"));
                    return;
                }

                if (block.getType() == Material.WATER_CAULDRON) block.setType(Material.CAULDRON);
                plugin.getRevivalManager().spawnBloodLiquid(block, targetName);

                block.getWorld().playSound(block.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1, 1);
                event.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
                event.setCancelled(true);
            }
        }

        if (bloodEntity != null && item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
            String targetName = getTargetFromEntity(bloodEntity);
            if (targetName != null) {
                UUID targetUUID = findPlayerUUID(targetName);
                if (targetUUID != null) {
                    plugin.getRevivalManager().revivePlayer(Bukkit.getOfflinePlayer(targetUUID), block.getLocation().add(0.5, 1, 0.5));
                    bloodEntity.remove();
                    item.setAmount(item.getAmount() - 1);
                    block.getWorld().strikeLightningEffect(block.getLocation());
                    Bukkit.broadcastMessage(plugin.getConfigManager().getMsg("revive-broadcast").replace("{PLAYER}", targetName));
                }
            }
        }
    }

    @EventHandler public void onBlockBreak(BlockBreakEvent event) { plugin.getRevivalManager().cleanUpCauldron(event.getBlock()); }
    @EventHandler public void onEntityExplode(EntityExplodeEvent event) { event.blockList().forEach(plugin.getRevivalManager()::cleanUpCauldron); }
    @EventHandler public void onBlockExplode(BlockExplodeEvent event) { event.blockList().forEach(plugin.getRevivalManager()::cleanUpCauldron); }
    @EventHandler public void onPistonExtend(BlockPistonExtendEvent event) { event.getBlocks().forEach(plugin.getRevivalManager()::cleanUpCauldron); }
    @EventHandler public void onPistonRetract(BlockPistonRetractEvent event) { event.getBlocks().forEach(plugin.getRevivalManager()::cleanUpCauldron); }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getScoreboardTags().contains(BLOOD_TAG)) event.setCancelled(true);
    }

    private TextDisplay findBloodEntity(Block cauldron) {
        Collection<Entity> entities = cauldron.getWorld().getNearbyEntities(
                cauldron.getLocation().add(0.5, 0.5, 0.5), 1.0, 1.0, 1.0
        );
        for (Entity entity : entities) {
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains(BLOOD_TAG)) {
                return (TextDisplay) entity;
            }
        }
        return null;
    }

    private String getTargetFromEntity(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(TARGET_PREFIX)) {
                return tag.substring(TARGET_PREFIX.length());
            }
        }
        return null;
    }

    private UUID findPlayerUUID(String name) {
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.getUniqueId() != null) {
                return offlinePlayer.getUniqueId();
            }
        } catch (Exception ignored) {}
        return null;
    }
}