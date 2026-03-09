package pl.Dowimixworsafe.reviveMe.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import pl.Dowimixworsafe.reviveMe.ReviveMe;
import pl.Dowimixworsafe.reviveMe.managers.*;

import java.util.UUID;

public class ReviveMeCommand implements CommandExecutor {

    private final ReviveMe plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final RevivalManager revivalManager;
    private final PunishmentManager punishmentManager;

    public ReviveMeCommand(ReviveMe plugin, ConfigManager configManager, DataManager dataManager, RevivalManager revivalManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.revivalManager = revivalManager;
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(configManager.getMsg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "--- ReviveMe Admin ---");
            sender.sendMessage(ChatColor.GOLD + "/reviveme <nick>" + ChatColor.GRAY + " - " + configManager.getMsg("cmd-help-revive"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme reload" + ChatColor.GRAY + " - " + configManager.getMsg("cmd-help-reload"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set mode <ghost/ban/spectator>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set time <min>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set mob <TYPE>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set fly <true/false>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set glow <true/false>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set haunt <true/false>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set haunt-delay <seconds>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            configManager.reload();
            dataManager.createDataFile();
            sender.sendMessage(configManager.getMsg("reload-success"));
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Uzycie: /reviveme set <opcja> <wartosc>");
                return true;
            }
            String option = args[1].toLowerCase();
            String value = args[2];

            if (option.equals("lang")) {
                plugin.getConfig().set("language", value);
                plugin.saveConfig();
                configManager.reload();
                sender.sendMessage(ChatColor.GREEN + "Language changed to: " + value);
                return true;
            }

            if (option.equals("mode")) {
                if (value.equalsIgnoreCase("ghost") || value.equalsIgnoreCase("ban") || value.equalsIgnoreCase("spectator")) {
                    plugin.getConfig().set("punishment-mode", value.toLowerCase());
                    plugin.saveConfig();
                    sender.sendMessage(configManager.getMsg("mode-changed").replace("{MODE}", value.toUpperCase()));
                    refreshGhosts();
                } else {
                    sender.sendMessage(ChatColor.RED + "Modes: ghost, ban, spectator");
                }
                return true;
            }

            if (option.equals("time")) {
                try {
                    int minutes = Integer.parseInt(value);
                    plugin.getConfig().set("ban-duration-minutes", minutes);
                    plugin.saveConfig();
                    sender.sendMessage(configManager.getMsg("time-changed").replace("{TIME}", String.valueOf(minutes)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Error: Invalid number.");
                }
                return true;
            }

            if (option.equals("mob")) {
                try {
                    EntityType type = EntityType.valueOf(value.toUpperCase());
                    plugin.getConfig().set("ghost-mob-type", type.name());
                    plugin.saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "Ghost mob changed to: " + type.name());
                    refreshGhosts();
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid mob type! Use e.g. ALLAY, VEX, PARROT.");
                }
                return true;
            }

            if (option.equals("fly")) {
                boolean fly = Boolean.parseBoolean(value);
                plugin.getConfig().set("ghost-fly", fly);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Ghost fly set to: " + fly);
                return true;
            }

            if (option.equals("glow")) {
                boolean glow = Boolean.parseBoolean(value);
                plugin.getConfig().set("ghost-glowing", glow);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Ghost glowing set to: " + glow);
                refreshGhosts();
                return true;
            }

            if (option.equals("haunt")) {
                boolean haunt = Boolean.parseBoolean(value);
                plugin.getConfig().set("ghost-haunt-enabled", haunt);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Ghost haunting set to: " + haunt);
                refreshGhosts();
                return true;
            }

            if (option.equals("haunt-delay")) {
                try {
                    int seconds = Integer.parseInt(value);
                    plugin.getConfig().set("ghost-haunt-cooldown", seconds);
                    plugin.saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "Haunt delay set to: " + seconds + "s");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number.");
                }
                return true;
            }
        }

        UUID targetUUID = findUUID(args[0]);

        if (targetUUID == null) {
            sender.sendMessage(ChatColor.RED + "Gracz nieznany.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        if (dataManager.isDead(targetUUID)) {
            Location loc = (sender instanceof Player) ? ((Player) sender).getLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
            revivalManager.revivePlayer(target, loc);
            sender.sendMessage(configManager.getMsg("admin-revive-success").replace("{PLAYER}", target.getName()));
        } else {
            sender.sendMessage(configManager.getMsg("player-not-dead"));
        }

        return true;
    }

    private void refreshGhosts() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (dataManager.isDead(p)) {
                punishmentManager.applyPunishmentMode(p);
            }
        }
    }

    private UUID findUUID(String name) {
        Player p = Bukkit.getPlayer(name);
        if (p != null) return p.getUniqueId();
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            if (op.hasPlayedBefore() || op.getUniqueId() != null) {
                return op.getUniqueId();
            }
        } catch (Exception ignored) {}
        return null;
    }
}