package pl.Dowimixworsafe.reviveRitual.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import pl.Dowimixworsafe.reviveRitual.utils.TimeUtils;
import org.bukkit.util.StringUtil;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;
import pl.Dowimixworsafe.reviveRitual.managers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReviveRitualCommand implements CommandExecutor, TabCompleter {

    private final ReviveRitual plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final RevivalManager revivalManager;
    private final PunishmentManager punishmentManager;

    public ReviveRitualCommand(ReviveRitual plugin, ConfigManager configManager, DataManager dataManager,
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
            sender.sendMessage(configManager.getMsg("cmd-help-header"));
            sender.sendMessage(ChatColor.GOLD + "/rr <nick>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-revive"));
            sender.sendMessage(ChatColor.GOLD + "/rr reload" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-reload"));
            sender.sendMessage(ChatColor.GOLD + "/rr set lang <pl/en>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-lang"));
            sender.sendMessage(ChatColor.GOLD + "/rr set mode <ghost/ban/spectator>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-mode"));
            sender.sendMessage(ChatColor.GOLD + "/rr set time <min>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-time"));
            sender.sendMessage(ChatColor.GOLD + "/rr set mob <TYPE>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-mob"));
            sender.sendMessage(ChatColor.GOLD + "/rr set fly <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-fly"));
            sender.sendMessage(ChatColor.GOLD + "/rr set glow <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-glow"));
            sender.sendMessage(ChatColor.GOLD + "/rr set haunt <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-haunt"));
            sender.sendMessage(ChatColor.GOLD + "/rr set haunt-delay <seconds>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-haunt-delay"));
            sender.sendMessage(ChatColor.GOLD + "/rr set graves <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-graves"));
            sender.sendMessage(ChatColor.GOLD + "/rr set grave-coords <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-grave-coords"));
            sender.sendMessage(ChatColor.GOLD + "/rr set cross-loot <true/false>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-cross-loot"));
            sender.sendMessage(ChatColor.GOLD + "/rr cleargraves <all/nick/radius>" + ChatColor.GRAY + " - "
                    + configManager.getMsg("cmd-help-cleargraves"));
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
                long parsedTime = TimeUtils.parseTimeString(value);
                if (parsedTime > 0) {
                    plugin.getConfig().set("punishment-time", value);
                    plugin.saveConfig();
                    sender.sendMessage(configManager.getMsg("time-changed").replace("{TIME}", value));
                } else {
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

            if (option.equals("grave-coords")) {
                boolean coords = Boolean.parseBoolean(value);
                plugin.getConfig().set("grave-show-coordinates", coords);
                plugin.saveConfig();
                sender.sendMessage(
                        configManager.getMsg("grave-coords-changed").replace("{STATE}", String.valueOf(coords)));
                return true;
            }

            if (option.equals("cross-loot")) {
                boolean cross = Boolean.parseBoolean(value);
                plugin.getConfig().set("grave-cross-loot", cross);
                plugin.saveConfig();
                sender.sendMessage(
                        configManager.getMsg("cross-loot-changed").replace("{STATE}", String.valueOf(cross)));
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("cleargraves")) {
            if (args.length < 2) {
                sender.sendMessage(configManager.getMsg("cleargraves-usage"));
                return true;
            }
            String arg = args[1];

            if (arg.equalsIgnoreCase("all")) {
                int count = 0;
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    for (org.bukkit.entity.Entity e : world.getEntities()) {
                        for (String tag : e.getScoreboardTags()) {
                            if (tag.startsWith("grave_")) {
                                e.remove();
                                count++;
                                break;
                            }
                        }
                    }
                }
                sender.sendMessage(configManager.getMsg("cleargraves-all").replace("{COUNT}", String.valueOf(count)));
                return true;
            }

            UUID clearTarget = findUUID(arg);
            if (clearTarget != null) {
                String prefix = "grave_" + clearTarget.toString();
                int count = 0;
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    for (org.bukkit.entity.Entity e : world.getEntities()) {
                        for (String tag : e.getScoreboardTags()) {
                            if (tag.startsWith(prefix)) {
                                e.remove();
                                count++;
                                break;
                            }
                        }
                    }
                }
                sender.sendMessage(configManager.getMsg("cleargraves-player").replace("{COUNT}", String.valueOf(count))
                        .replace("{PLAYER}", arg));
                return true;
            }

            if (sender instanceof Player) {
                try {
                    int radius = Integer.parseInt(arg);
                    Player p = (Player) sender;
                    int count = 0;
                    for (org.bukkit.entity.Entity e : p.getNearbyEntities(radius, radius, radius)) {
                        for (String tag : e.getScoreboardTags()) {
                            if (tag.startsWith("grave_")) {
                                e.remove();
                                count++;
                                break;
                            }
                        }
                    }
                    sender.sendMessage(configManager.getMsg("cleargraves-radius")
                            .replace("{COUNT}", String.valueOf(count)).replace("{RADIUS}", String.valueOf(radius)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(configManager.getMsg("cleargraves-invalid").replace("{ARG}", arg));
                }
            } else {
                sender.sendMessage(configManager.getMsg("cleargraves-console"));
            }
            return true;
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
            revivalManager.revivePlayer(target, loc, false);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp())
            return List.of();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("reload", "set", "cleargraves"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[0], options, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            List<String> options = Arrays.asList(
                    "lang", "mode", "time", "mob", "fly", "glow",
                    "haunt", "haunt-delay", "graves", "grave-coords", "cross-loot");
            StringUtil.copyPartialMatches(args[1], options, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("cleargraves")) {
            List<String> options = new ArrayList<>(Arrays.asList("all", "10", "25", "50", "100"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                options.add(p.getName());
            }
            StringUtil.copyPartialMatches(args[1], options, completions);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            String option = args[1].toLowerCase();
            switch (option) {
                case "lang":
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("pl", "en"), completions);
                    break;
                case "mode":
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("ghost", "ban", "spectator"), completions);
                    break;
                case "fly":
                case "glow":
                case "haunt":
                case "graves":
                case "grave-coords":
                case "cross-loot":
                    StringUtil.copyPartialMatches(args[2], Arrays.asList("true", "false"), completions);
                    break;
                case "mob":
                    List<String> mobs = Arrays.stream(EntityType.values())
                            .filter(EntityType::isAlive)
                            .map(e -> e.name().toLowerCase())
                            .collect(Collectors.toList());
                    StringUtil.copyPartialMatches(args[2], mobs, completions);
                    break;
                case "time":
                    completions.addAll(Arrays.asList("30s", "5m", "10m", "1h", "2d"));
                    break;
                case "haunt-delay":
                    completions.addAll(Arrays.asList("15", "30", "45", "60", "120"));
                    break;
            }
        }

        return completions;
    }
}