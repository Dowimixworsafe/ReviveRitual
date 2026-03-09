package pl.Dowimixworsafe.reviveMe.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import pl.Dowimixworsafe.reviveMe.ReviveMe;

public class SpectatorListener implements Listener {

    private final ReviveMe plugin;

    public SpectatorListener(ReviveMe plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpectatorInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDataManager().isDead(player) && plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
            if (event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
                plugin.getPunishmentManager().openSpectatorGUI(player);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (plugin.getDataManager().isDead(e.getPlayer()) && plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
            if (e.isSneaking()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (e.getPlayer().getSpectatorTarget() == null) {
                            plugin.getPunishmentManager().openSpectatorGUI(e.getPlayer());
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (plugin.getDataManager().isDead(e.getPlayer()) && plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
            if (e.getPlayer().getSpectatorTarget() == null && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                if (!e.getPlayer().getOpenInventory().getTitle().contains("Spectator")) {
                    plugin.getPunishmentManager().openSpectatorGUI(e.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClickSpectator(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        if (plugin.getDataManager().isDead(player)) {
            e.setCancelled(true);
            if (e.getView().getTitle().contains("Spectator: Wybierz gracza")) {
                if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                    SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                    if (meta.getOwningPlayer() != null) {
                        Player target = meta.getOwningPlayer().getPlayer();
                        if (target != null && target.isOnline()) {
                            player.closeInventory();
                            player.teleport(target);
                            player.setGameMode(GameMode.SPECTATOR);
                            player.setSpectatorTarget(target);
                        }
                    }
                }
            }
        }
    }
}