package pl.Dowimixworsafe.reviveMe.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import pl.Dowimixworsafe.reviveMe.ReviveMe;
import pl.Dowimixworsafe.reviveMe.utils.TimeUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PunishmentManager {

    private final ReviveMe plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final GhostManager ghostManager;
    private final Map<UUID, Long> hauntCooldowns = new HashMap<>();

    public PunishmentManager(ReviveMe plugin, ConfigManager configManager, DataManager dataManager,
            GhostManager ghostManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.ghostManager = ghostManager;
    }

    public void applyPunishmentMode(Player player) {
        String mode = plugin.getConfig().getString("punishment-mode");
        if (mode.equalsIgnoreCase("ban"))
            return;

        long reviveTime = dataManager.getData().getLong("reviveTime." + player.getUniqueId(), 0);
        long timeLeft = reviveTime - System.currentTimeMillis();

        if (timeLeft <= 0) {
            org.bukkit.Location respawnLoc = player.getRespawnLocation();
            if (respawnLoc == null)
                respawnLoc = player.getWorld().getSpawnLocation();
            plugin.getRevivalManager().revivePlayer(player, respawnLoc);
            return;
        }

        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);

        if (mode.equalsIgnoreCase("spectator")) {
            enableSpectatorMode(player);
        } else {
            enableGhostMode(player);
        }
    }

    public void enableSpectatorMode(Player player) {
        List<Player> alivePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !dataManager.isDead(p) && p != player)
                .collect(Collectors.toList());

        if (alivePlayers.isEmpty()) {
            long reviveTime = dataManager.getData().getLong("reviveTime." + player.getUniqueId(), 0);
            long timeLeft = reviveTime - System.currentTimeMillis();

            String timeString = TimeUtils.formatTime(timeLeft);

            player.kickPlayer(configManager.getMsg("kick-spectator-no-players")
                    .replace("{TIME}", timeString).replace("\\n", "\n"));
            return;
        }

        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        ghostManager.removeGhostEntity(player);

        if (!alivePlayers.isEmpty()) {
            player.setSpectatorTarget(alivePlayers.get(0));
        }

        ItemStack comp = new ItemStack(Material.COMPASS);
        ItemMeta meta = comp.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Menu Spectatora (Prawy PM)");
        comp.setItemMeta(meta);
        player.getInventory().setItem(0, comp);
        player.getInventory().setItem(8, comp);

        openSpectatorGUI(player);
    }

    public void enableGhostMode(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        fillGhostInventory(player);
        ghostManager.spawnGhostEntity(player);

        boolean canFly = plugin.getConfig().getBoolean("ghost-fly", true);
        player.setAllowFlight(canFly);
        player.setFlying(canFly);
        player.getInventory().setHeldItemSlot(0);
    }

    public void openSpectatorGUI(Player player) {
        List<Player> alivePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !dataManager.isDead(p) && p != player)
                .collect(Collectors.toList());

        if (alivePlayers.isEmpty())
            return;

        int size = (int) Math.ceil(alivePlayers.size() / 9.0) * 9;
        if (size > 54)
            size = 54;
        if (size == 0)
            size = 9;

        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, size,
                ChatColor.DARK_BLUE + "Spectator: Wybierz gracza");

        for (Player target : alivePlayers) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.YELLOW + target.getName());
            head.setItemMeta(meta);
            gui.addItem(head);
        }
        player.openInventory(gui);
    }

    public void handleHaunt(Player player) {
        long cooldownTime = plugin.getConfig().getInt("ghost-haunt-cooldown", 45) * 1000L;
        long lastUse = hauntCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeLeft = (lastUse + cooldownTime) - System.currentTimeMillis();

        if (timeLeft > 0) {
            player.sendMessage(configManager.getMsg("haunt-cooldown").replace("{TIME}",
                    String.valueOf(TimeUnit.MILLISECONDS.toSeconds(timeLeft))));
            return;
        }

        player.getWorld().playSound(player.getLocation(), Sound.AMBIENT_CAVE, 2.0f, 0.5f);
        player.sendMessage(configManager.getMsg("haunt-used"));
        hauntCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void fillGhostInventory(Player player) {
        ItemStack haunt = new ItemStack(Material.POLISHED_BLACKSTONE_BUTTON);
        ItemMeta hMeta = haunt.getItemMeta();
        hMeta.setDisplayName(configManager.getMsg("haunt-item-name"));
        haunt.setItemMeta(hMeta);

        boolean hauntEnabled = plugin.getConfig().getBoolean("ghost-haunt-enabled", true);

        if (hauntEnabled) {
            player.getInventory().setItem(8, haunt);
        }
    }

    public void removeGhostAttributes(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setSpectatorTarget(null);
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);

        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setAllowFlight(false);
        player.setFlying(false);

        ghostManager.removeGhostEntity(player);
    }
}