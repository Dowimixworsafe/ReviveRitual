package pl.Dowimixworsafe.reviveRitual.managers;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;

import java.util.Collection;

public class RevivalManager {

    private final ReviveRitual plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    private final String BLOOD_TAG = "custom_blood_liquid";

    public RevivalManager(ReviveRitual plugin, DataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
    }

    public void revivePlayer(OfflinePlayer target, Location respawnLoc) {
        dataManager.getData().set("status." + target.getUniqueId(), "alive");
        dataManager.getData().set("reviveTime." + target.getUniqueId(), null);
        dataManager.saveData();

        if (target.isOnline()) {
            Player p = target.getPlayer();
            if (p != null) {
                p.teleport(respawnLoc);
                p.sendMessage(configManager.getMsg("revive-success"));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                p.setGameMode(GameMode.SURVIVAL);
                p.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
                p.setAllowFlight(false);
                p.setFlying(false);
                p.getInventory().clear();

                plugin.getPunishmentManager().removeGhostAttributes(p);
            }
        }
    }

    public void spawnBloodLiquid(Block cauldron, String targetName) {
        Location spawnLoc = cauldron.getLocation().toCenterLocation();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(-90);

        TextDisplay display = (TextDisplay) cauldron.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
        display.addScoreboardTag(BLOOD_TAG);
        display.addScoreboardTag("target_player_" + targetName);

        String nbtData = "{transformation:[3.8f,0.0f,0.0f,-0.1f, 0.0f,3.8f,0.0f,-0.56f, 0.0f,0.0f,3.8f,0.40f, 0.0f,0.0f,0.0f,1.0f], billboard:\"fixed\", background:0, text:{sprite:\"block/lava_still\", color:\"#530001\"}}";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "data merge entity " + display.getUniqueId() + " " + nbtData);
    }

    public void cleanUpCauldron(Block block) {
        if (block.getType() == Material.CAULDRON || block.getType() == Material.WATER_CAULDRON) {
            Collection<Entity> entities = block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 1.0, 1.0, 1.0);
            for (Entity e : entities) {
                if (e.getScoreboardTags().contains(BLOOD_TAG)) {
                    e.remove();
                    block.getWorld().spawnParticle(Particle.LARGE_SMOKE, block.getLocation().add(0.5, 0.5, 0.5), 10);
                }
            }
        }
    }
}