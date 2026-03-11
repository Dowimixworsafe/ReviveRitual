package pl.Dowimixworsafe.reviveRitual.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import org.bukkit.event.Listener;
import pl.Dowimixworsafe.reviveRitual.utils.TimeUtils;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;

import java.util.UUID;

    public class DeathListener implements Listener {

    private final ReviveRitual plugin;

    public DeathListener(ReviveRitual plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        int deaths = plugin.getDataManager().getData().getInt("deaths." + uuid, 0) + 1;
        plugin.getDataManager().getData().set("deaths." + uuid, deaths);
        plugin.getDataManager().getData().set("status." + uuid, "dead");

        if (plugin.getConfig().getBoolean("graves-enabled", true)) {
            org.bukkit.Location graveLoc = plugin.getTombstoneManager().createGrave(player);
            event.getDrops().clear();

            if (plugin.getConfig().getBoolean("grave-show-coordinates", true)) {
                String msg = plugin.getConfigManager().getMsg("grave-coordinates")
                        .replace("{X}", String.valueOf(graveLoc.getBlockX()))
                        .replace("{Y}", String.valueOf(graveLoc.getBlockY()))
                        .replace("{Z}", String.valueOf(graveLoc.getBlockZ()));
                player.sendMessage(msg);
            }
        }

        event.setKeepLevel(true);
        event.setDroppedExp(0);

        String timeConfig = plugin.getConfig().getString("punishment-time", plugin.getConfig().getString("punishment-time-minutes", "180m"));
        long durationMillis = TimeUtils.parseTimeString(timeConfig);
        if (durationMillis <= 0) durationMillis = 180 * 60 * 1000L; // Fallback to 3h if parse fails
        plugin.getDataManager().getData().set("reviveTime." + uuid, System.currentTimeMillis() + durationMillis);
        plugin.getDataManager().saveData();

        String mode = plugin.getConfig().getString("punishment-mode");

        if (mode.equalsIgnoreCase("ban")) {
            String timeString = TimeUtils.formatTime(durationMillis);

            String kickMessage = plugin.getConfigManager().getMsg("kick-message-layout")
                    .replace("{TIME}", timeString)
                    .replace("\\n", "\n");

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.kickPlayer(kickMessage);
                }
            }.runTaskLater(plugin, 2L);
        } else {
            player.sendMessage(plugin.getConfigManager().getMsg("ghost-mode-start"));
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.spigot().respawn();
                    plugin.getPunishmentManager().applyPunishmentMode(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (plugin.getDataManager().isDead(event.getPlayer())) {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> plugin.getPunishmentManager().applyPunishmentMode(event.getPlayer()), 2L);
        }
    }
}