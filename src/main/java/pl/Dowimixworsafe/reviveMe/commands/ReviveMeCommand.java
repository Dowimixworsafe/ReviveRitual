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

    public ReviveMeCommand(ReviveMe plugin, ConfigManager configManager, DataManager dataManager,
            RevivalManager revivalManager, PunishmentManager punishmentManager) {
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
            sender.sendMessage(ChatColor.GOLD + "/reviveme <nick>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-revive"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme reload" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-reload"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set lang <pl/en>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-lang"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set mode <ghost/ban/spectator>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-mode"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set time <min>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-time"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set mob <TYPE>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-mob"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set fly <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-fly"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set glow <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-glow"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set haunt <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-haunt"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set haunt-delay <seconds>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-haunt-delay"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set graves <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-graves"));
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
                sender.sendMessage(configManager.getMsg("cmd-usage-set"));
                return true;
            }
            String option = args[1].toLowerCase();
            String value = args[2];

            if (option.equals("lang")) {
                plugin.getConfig().set("language", value);
                plugin.saveConfig();
                configManager.reload();
                sender.sendMessage(configManager.getMsg("lang-changed").replace("{LANG}", value));
                return true;
            }

            if (option.equals("mode")) {
                if (value.equalsIgnoreCase("ghost") || value.equalsIgnoreCase("ban")
                        || value.equalsIgnoreCase("spectator")) {
                    plugin.getConfig().set("punishment-mode", value.toLowerCase());
                    plugin.saveConfig();
                    sender.sendMessage(configManager.getMsg("mode-changed").replace("{MODE}", value.toUpperCase()));
                    refreshGhosts();
                } else {
                    sender.sendMessage(configManager.getMsg("mode-invalid"));
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
                    sender.sendMessage(configManager.getMsg("time-invalid"));
                }
                return true;
            }

            if (option.equals("mob")) {
                try {
                    EntityType type = EntityType.valueOf(value.toUpperCase());
                    plugin.getConfig().set("ghost-mob-type", type.name());
                    plugin.saveConfig();
                    sender.sendMessage(configManager.getMsg("mob-changed").replace("{MOB}", type.name()));
                    refreshGhosts();
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(configManager.getMsg("mob-invalid"));
                }
                return true;
            }

            if (option.equals("fly")) {
                boolean fly = Boolean.parseBoolean(value);
                plugin.getConfig().set("ghost-fly", fly);
                plugin.saveConfig();
                sender.sendMessage(configManager.getMsg("fly-changed").replace("{STATE}", String.valueOf(fly)));
                return true;
            }

            if (option.equals("glow")) {
                boolean glow = Boolean.parseBoolean(value);
                plugin.getConfig().set("ghost-glowing", glow);
                plugin.saveConfig();
                sender.sendMessage(configManager.getMsg("glow-changed").replace("{STATE}", String.valueOf(glow)));
                refreshGhosts();
                return true;
            }

            if (option.equals("haunt")) {
                boolean haunt = Boolean.parseBoolean(value);
                plugin.getConfig().set("ghost-haunt-enabled", haunt);
                plugin.saveConfig();
                sender.sendMessage(configManager.getMsg("haunt-set").replace("{STATE}", String.valueOf(haunt)));
                refreshGhosts();
                return true;
            }

            if (option.equals("haunt-delay")) {
                try {
                    int seconds = Integer.parseInt(value);
                    plugin.getConfig().set("ghost-haunt-cooldown", seconds);
                    plugin.saveConfig();
                    sender.sendMessage(
                            configManager.getMsg("haunt-delay-changed").replace("{TIME}", String.valueOf(seconds)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(configManager.getMsg("time-invalid"));
                }
                return true;
            }

            if (option.equals("graves")) {
                boolean graves = Boolean.parseBoolean(value);
                plugin.getConfig().set("graves-enabled", graves);
                plugin.saveConfig();
                sender.sendMessage(configManager.getMsg("graves-changed").replace("{STATE}", String.valueOf(graves)));
                return true;
            }
        }

        UUID targetUUID = findUUID(args[0]);

        if (targetUUID == null) {
            sender.sendMessage(configManager.getMsg("player-unknown"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        if (dataManager.isDead(targetUUID)) {
            Location loc = (sender instanceof Player) ? ((Player) sender).getLocation()
                    : Bukkit.getWorlds().get(0).getSpawnLocation();
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
        if (p != null)
            return p.getUniqueId();
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            if (op.hasPlayedBefore() || op.getUniqueId() != null) {
                return op.getUniqueId();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}