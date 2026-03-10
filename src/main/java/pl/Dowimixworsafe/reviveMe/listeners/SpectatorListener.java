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
        if (plugin.getDataManager().isDead(player)
                && plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
            if (event.getAction().name().contains("RIGHT") || event.getAction().name().contains("LEFT")) {
                plugin.getPunishmentManager().openSpectatorGUI(player);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (plugin.getDataManager().isDead(e.getPlayer())
                && plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
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
        if (plugin.getDataManager().isDead(e.getPlayer())
                && plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
            if (e.getPlayer().getSpectatorTarget() == null && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                String rawTitle = plugin.getConfigManager().getMsg("gui-spectator-title");
                String titlePrefix = rawTitle.contains("{PAGE}") ? rawTitle.substring(0, rawTitle.indexOf("{PAGE}"))
                        : rawTitle;

                if (!e.getPlayer().getOpenInventory().getTitle().startsWith(titlePrefix)
                        && !e.getPlayer().getOpenInventory().getTitle().contains("Spectator")) {
                    plugin.getPunishmentManager().openSpectatorGUI(e.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        String rawTitle = plugin.getConfigManager().getMsg("gui-spectator-title");
        String titlePrefix = rawTitle.contains("{PAGE}") ? rawTitle.substring(0, rawTitle.indexOf("{PAGE}")) : rawTitle;

        if (event.getView().getTitle().startsWith(titlePrefix) || event.getView().getTitle().contains("Spectator")) {
            event.setCancelled(true);

            if (!plugin.getDataManager().isDead(player)
                    || !plugin.getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
                player.closeInventory();
                return;
            }

            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null && meta.getOwningPlayer().isOnline()) {
                    Player target = meta.getOwningPlayer().getPlayer();
                    if (target != null) {
                        player.closeInventory();
                        player.teleport(target);
                        player.sendMessage(plugin.getConfigManager().getMsg("gui-spectator-teleport")
                                .replace("{PLAYER}", target.getName()));
                        player.setGameMode(GameMode.SPECTATOR);
                        player.setSpectatorTarget(target);
                    }
                }
            } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                String name = event.getCurrentItem().getItemMeta().getDisplayName();
                String title = event.getView().getTitle();
                int page = 1;
                try {
                    String numStr = title.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        page = Integer.parseInt(numStr);
                    }
                } catch (Exception ignored) {
                }

                if (name.equals(plugin.getConfigManager().getMsg("gui-spectator-next"))) {
                    plugin.getPunishmentManager().openSpectatorGUI(player, page + 1);
                } else if (name.equals(plugin.getConfigManager().getMsg("gui-spectator-prev"))) {
                    plugin.getPunishmentManager().openSpectatorGUI(player, page - 1);
                }
            }
        }
    }
}