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

    public void revivePlayer(OfflinePlayer target, Location respawnLoc, boolean withRitual) {
        if (target.isOnline()) {
            Player p = target.getPlayer();
            if (p != null) {
                if (withRitual) {
                    dataManager.getData().set("status." + target.getUniqueId(), "reviving");
                    dataManager.saveData();
                    playReviveAnimation(p, target, respawnLoc);
                } else {
                    executeReviveBase(p, target, respawnLoc);
                }
            }
        }
    }

    private void executeReviveBase(Player player, OfflinePlayer target, Location respawnLoc) {
        dataManager.getData().set("status." + target.getUniqueId(), "alive");
        dataManager.getData().set("reviveTime." + target.getUniqueId(), null);
        dataManager.saveData();

        plugin.getPunishmentManager().removeGhostAttributes(player);
        player.teleport(respawnLoc);
        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.getInventory().clear();
        player.sendMessage(configManager.getMsg("revive-success"));
    }

    private void playReviveAnimation(Player player, OfflinePlayer target, Location respawnLoc) {
        Location loc = respawnLoc.clone();
        org.bukkit.World world = loc.getWorld();

        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.5f);
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40) {
                    cancel();
                    return;
                }
                double angle = ticks * 0.3;
                double radius = 1.5 - (ticks * 0.02);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = ticks * 0.05;
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x, y, z), 2, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(-x, y, -z), 2, 0.05, 0.05, 0.05, 0.01);

                if (ticks % 5 == 0) {
                    world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 0.1, 0), 8, 0.5, 0.1, 0.5, 0.02);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            executeReviveBase(player, target, respawnLoc);

            world.strikeLightningEffect(respawnLoc);

            world.spawnParticle(Particle.TOTEM_OF_UNDYING, respawnLoc.clone().add(0, 1, 0), 100, 0.5, 1.0, 0.5, 0.5);

            world.playSound(respawnLoc, Sound.ITEM_TOTEM_USE, 1f, 1f);
        }, 40L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 20) {
                        cancel();
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                        return;
                    }
                    Location pLoc = player.getLocation().add(0, 1, 0);
                    for (int i = 0; i < 8; i++) {
                        double angle = (ticks * 0.4) + (i * Math.PI / 4);
                        double radius = 0.5 + (ticks * 0.1);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        world.spawnParticle(Particle.END_ROD, pLoc.clone().add(x, 0, z), 1, 0, 0.1, 0, 0.02);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }, 50L);
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
            Collection<Entity> entities = block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5),
                    1.0, 1.0, 1.0);
            for (Entity e : entities) {
                if (e.getScoreboardTags().contains(BLOOD_TAG)) {
                    e.remove();
                    block.getWorld().spawnParticle(Particle.LARGE_SMOKE, block.getLocation().add(0.5, 0.5, 0.5), 10);
                }
            }
        }
    }
}