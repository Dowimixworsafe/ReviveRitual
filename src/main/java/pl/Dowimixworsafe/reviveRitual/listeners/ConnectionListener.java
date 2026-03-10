package pl.Dowimixworsafe.reviveRitual.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;
import pl.Dowimixworsafe.reviveRitual.utils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConnectionListener implements Listener {

    private final ReviveRitual plugin;

    public ConnectionListener(ReviveRitual plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("ban")) {
            return;
        }

        UUID uuid = event.getUniqueId();
        String status = plugin.getDataManager().getData().getString("status." + uuid, "alive");

        if (status.equals("dead")) {
            long reviveTime = plugin.getDataManager().getData().getLong("reviveTime." + uuid, 0);
            long timeLeft = reviveTime - System.currentTimeMillis();

            if (timeLeft > 0) {
                Date expireDate = new Date(reviveTime);
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                String timeString = TimeUtils.formatTime(timeLeft);

                String message = plugin.getConfigManager().getMsg("kick-screen-layout")
                        .replace("{DATE}", format.format(expireDate))
                        .replace("{TIME}", timeString)
                        .replace("\\n", "\n");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            } else {
                plugin.getDataManager().getData().set("status." + uuid, "alive");
                plugin.getDataManager().getData().set("reviveTime." + uuid, null);
                plugin.getDataManager().saveData();
                event.allow();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getDataManager().isDead(event.getPlayer())) {
            plugin.getPunishmentManager().applyPunishmentMode(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getGhostManager().removeGhostEntity(event.getPlayer());
    }
}