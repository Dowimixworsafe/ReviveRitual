package pl.Dowimixworsafe.reviveMe.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.Dowimixworsafe.reviveMe.ReviveMe;
import pl.Dowimixworsafe.reviveMe.utils.TimeUtils;

import java.util.UUID;

public class DeathListener implements Listener {

    private final ReviveMe plugin;

    public DeathListener(ReviveMe plugin) {
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
            plugin.getTombstoneManager().createGrave(player);
            event.getDrops().clear();
        }

        event.setKeepLevel(true);
        event.setDroppedExp(0);

        long minutes = plugin.getConfig().getInt("ban-duration-minutes", 180);
        long durationMillis = minutes * 60 * 1000L;
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