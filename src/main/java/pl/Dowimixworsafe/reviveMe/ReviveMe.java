package pl.Dowimixworsafe.reviveMe;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class ReviveMe extends JavaPlugin implements Listener, CommandExecutor {

    private File dataFile;
    private FileConfiguration dataConfig;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    private Team ghostTeam;

    private final String BLOOD_TAG = "custom_blood_liquid";
    private final String TARGET_PREFIX = "target_player_";

    private final Map<UUID, UUID> ghostEntities = new HashMap<>();
    private final Map<UUID, Long> hauntCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        createDataFile();

        getConfig().addDefault("language", "pl");
        getConfig().addDefault("punishment-mode", "ghost");
        getConfig().addDefault("ban-duration-minutes", 180);

        getConfig().addDefault("ghost-mob-type", "ALLAY");
        getConfig().addDefault("ghost-nametag-visible", true);
        getConfig().addDefault("ghost-glowing", true);
        getConfig().addDefault("ghost-fly", true);

        getConfig().addDefault("ghost-haunt-enabled", true);
        getConfig().addDefault("ghost-haunt-cooldown", 45);

        getConfig().options().copyDefaults(true);
        saveConfig();

        createMessagesFile();

        setupGhostTeam();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("reviveme").setExecutor(this);

        startGhostTask();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isDead(p)) {
                applyPunishmentMode(p);
            }
        }
    }

    @Override
    public void onDisable() {
        saveData();
        for (UUID entityId : ghostEntities.values()) {
            Entity e = Bukkit.getEntity(entityId);
            if (e != null) e.remove();
        }
        ghostEntities.clear();
        hauntCooldowns.clear();
    }

    private UUID findPlayerUUID(String name) {
        Player onlinePlayer = Bukkit.getPlayer(name);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.getUniqueId() != null) {
                return offlinePlayer.getUniqueId();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(getMsg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "--- ReviveMe Admin ---");
            sender.sendMessage(ChatColor.GOLD + "/reviveme <nick>" + ChatColor.GRAY + " - " + getMsg("cmd-help-revive"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme reload" + ChatColor.GRAY + " - " + getMsg("cmd-help-reload"));
            sender.sendMessage(ChatColor.GOLD + "/reviveme set mode <ghost/ban/spectator>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set time <min>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set mob <TYPE>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set fly <true/false>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set glow <true/false>");
            sender.sendMessage(ChatColor.GOLD + "/reviveme set haunt <true/false>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            createDataFile();
            createMessagesFile();
            sender.sendMessage(getMsg("reload-success"));
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
                getConfig().set("language", value);
                saveConfig();
                createMessagesFile();
                sender.sendMessage(ChatColor.GREEN + "Language changed to: " + value);
                return true;
            }
            if (option.equals("mode")) {
                if (value.equalsIgnoreCase("ghost") || value.equalsIgnoreCase("ban") || value.equalsIgnoreCase("spectator")) {
                    getConfig().set("punishment-mode", value.toLowerCase());
                    saveConfig();
                    sender.sendMessage(getMsg("mode-changed").replace("{MODE}", value.toUpperCase()));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (isDead(p)) applyPunishmentMode(p);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Modes: ghost, ban, spectator");
                }
                return true;
            }
            if (option.equals("time")) {
                try {
                    int minutes = Integer.parseInt(value);
                    getConfig().set("ban-duration-minutes", minutes);
                    saveConfig();
                    sender.sendMessage(getMsg("time-changed").replace("{TIME}", String.valueOf(minutes)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Error: Invalid number.");
                }
                return true;
            }
            if (option.equals("mob")) {
                try {
                    EntityType type = EntityType.valueOf(value.toUpperCase());
                    getConfig().set("ghost-mob-type", type.name());
                    saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "Ghost mob changed to: " + type.name());
                    refreshAllGhosts();
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid mob type!");
                }
                return true;
            }
            if (option.equals("fly")) {
                boolean fly = Boolean.parseBoolean(value);
                getConfig().set("ghost-fly", fly);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Ghost fly set to: " + fly);
                return true;
            }
            if (option.equals("glow")) {
                boolean glow = Boolean.parseBoolean(value);
                getConfig().set("ghost-glowing", glow);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Ghost glowing set to: " + glow);
                refreshAllGhosts();
                return true;
            }
            if (option.equals("haunt")) {
                boolean haunt = Boolean.parseBoolean(value);
                getConfig().set("ghost-haunt-enabled", haunt);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Ghost haunting set to: " + haunt);
                refreshAllGhosts();
                return true;
            }
            if (option.equals("haunt-delay")) {
                try {
                    int seconds = Integer.parseInt(value);
                    getConfig().set("ghost-haunt-cooldown", seconds);
                    saveConfig();
                    sender.sendMessage(ChatColor.GREEN + "Haunt delay set to: " + seconds + "s");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number.");
                }
                return true;
            }
        }

        UUID targetUUID = findPlayerUUID(args[0]);

        if (targetUUID == null) {
            sender.sendMessage(ChatColor.RED + "Gracz nieznany.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

        if (dataConfig.getString("status." + targetUUID, "alive").equals("dead")) {
            Location loc = (sender instanceof Player) ? ((Player) sender).getLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
            revivePlayer(target, loc);
            sender.sendMessage(getMsg("admin-revive-success").replace("{PLAYER}", target.getName()));
        } else {
            sender.sendMessage(getMsg("player-not-dead"));
        }

        return true;
    }

    private void refreshAllGhosts() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isDead(p)) {
                applyPunishmentMode(p);
            }
        }
    }

    private void startGhostTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dataConfig.getConfigurationSection("status") == null) return;

                List<Player> alivePlayers = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !isDead(p))
                        .collect(Collectors.toList());

                for (String uuidStr : dataConfig.getConfigurationSection("status").getKeys(false)) {
                    if (!dataConfig.getString("status." + uuidStr).equals("dead")) continue;

                    UUID uuid = UUID.fromString(uuidStr);
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    long reviveTime = dataConfig.getLong("reviveTime." + uuid, 0);
                    long timeLeft = reviveTime - System.currentTimeMillis();

                    if (timeLeft <= 0) {
                        revivePlayer(player, player.getLocation());
                        continue;
                    } else {
                        String timeString = formatTime(timeLeft);
                        String actionMsg = getMsg("action-bar-timer").replace("{TIME}", timeString);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));
                    }

                    String mode = getConfig().getString("punishment-mode");

                    if (mode.equalsIgnoreCase("spectator")) {
                        if (alivePlayers.isEmpty()) {
                            String timeString = formatTime(timeLeft);
                            player.kickPlayer(getMsg("kick-spectator-no-players")
                                    .replace("{TIME}", timeString)
                                    .replace("\\n", "\n"));
                        }
                        removeGhostEntity(player);
                    } else if (mode.equalsIgnoreCase("ghost")) {
                        handleGhostEntity(player);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void handleGhostEntity(Player player) {
        UUID entityUUID = ghostEntities.get(player.getUniqueId());
        Entity ghostEntity = (entityUUID != null) ? Bukkit.getEntity(entityUUID) : null;

        if (ghostEntity == null || ghostEntity.isDead()) {
            spawnGhostEntity(player);
            return;
        }

        boolean canFly = getConfig().getBoolean("ghost-fly", true);
        Location loc = player.getLocation();

        if (canFly) ghostEntity.teleport(loc.add(0, 0.5, 0));
        else ghostEntity.teleport(loc);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));
    }

    private void removeGhostEntity(Player player) {
        if (ghostEntities.containsKey(player.getUniqueId())) {
            UUID entityId = ghostEntities.remove(player.getUniqueId());
            Entity e = Bukkit.getEntity(entityId);
            if (e != null) e.remove();
        }
    }

    private void spawnGhostEntity(Player player) {
        removeGhostEntity(player);

        String mobTypeStr = getConfig().getString("ghost-mob-type", "ALLAY").toUpperCase();
        EntityType type;
        try {
            type = EntityType.valueOf(mobTypeStr);
        } catch (IllegalArgumentException e) {
            type = EntityType.ALLAY;
        }

        Entity ghost = player.getWorld().spawnEntity(player.getLocation(), type);

        if (ghost instanceof LivingEntity) {
            ((LivingEntity) ghost).setAI(false);
        }

        ghost.setInvulnerable(true);
        ghost.setSilent(true);

        boolean glowing = getConfig().getBoolean("ghost-glowing", true);
        boolean nametag = getConfig().getBoolean("ghost-nametag-visible", true);

        ghost.setGlowing(glowing);

        if (nametag) {
            ghost.setCustomName(ChatColor.YELLOW + player.getName());
            ghost.setCustomNameVisible(true);
        } else {
            ghost.setCustomNameVisible(false);
        }

        if (ghost instanceof Allay) {
            ((Allay) ghost).setCanPickupItems(false);
        }

        player.hideEntity(this, ghost);

        ghostTeam.addEntry(ghost.getUniqueId().toString());
        ghostEntities.put(player.getUniqueId(), ghost.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity target = event.getRightClicked();
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (isDead(targetPlayer)) event.setCancelled(true);
        }
        if (ghostEntities.containsValue(target.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void openSpectatorGUI(Player player) {
        List<Player> alivePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !isDead(p) && p != player)
                .collect(Collectors.toList());

        if (alivePlayers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Brak graczy do oglądania.");
            return;
        }

        int size = (int) Math.ceil(alivePlayers.size() / 9.0) * 9;
        if (size == 0) size = 9;
        if (size > 54) size = 54;

        org.bukkit.inventory.Inventory gui = Bukkit.createInventory(null, size, ChatColor.DARK_BLUE + "Spectator: Wybierz gracza");

        for (Player target : alivePlayers) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.YELLOW + target.getName());
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Kliknij, aby oglądać"));
            head.setItemMeta(meta);
            gui.addItem(head);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (isDead(e.getPlayer()) && getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
            if (e.isSneaking()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (e.getPlayer().getSpectatorTarget() == null) {
                            openSpectatorGUI(e.getPlayer());
                        }
                    }
                }.runTaskLater(this, 1L);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (isDead(e.getPlayer()) && getConfig().getString("punishment-mode").equalsIgnoreCase("spectator")) {
            if (e.getPlayer().getSpectatorTarget() == null && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                if (!e.getPlayer().getOpenInventory().getTitle().contains("Spectator")) {
                    openSpectatorGUI(e.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClickSpectator(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        if (isDead(player)) {
            e.setCancelled(true);
            if (e.getView().getTitle().equals(ChatColor.DARK_BLUE + "Spectator: Wybierz gracza")) {
                if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                    SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                    if (meta.getOwningPlayer() != null) {
                        Player target = meta.getOwningPlayer().getPlayer();
                        if (target != null && target.isOnline()) {
                            player.closeInventory();
                            player.teleport(target);
                            player.setGameMode(GameMode.SPECTATOR);
                            player.setSpectatorTarget(target);
                            player.sendTitle("", ChatColor.GREEN + "Oglądasz: " + target.getName(), 10, 40, 10);
                        } else {
                            player.sendMessage(ChatColor.RED + "Gracz offline.");
                        }
                    }
                }
            }
        }
    }

    public void createMessagesFile() {
        if (!new File(getDataFolder(), "messages_pl.yml").exists()) {
            saveResource("messages_pl.yml", false);
        }
        if (!new File(getDataFolder(), "messages_en.yml").exists()) {
            saveResource("messages_en.yml", false);
        }

        String lang = getConfig().getString("language", "pl");
        String fileName = "messages_" + lang + ".yml";

        messagesFile = new File(getDataFolder(), fileName);

        if (!messagesFile.exists()) {
            if (getResource(fileName) != null) {
                saveResource(fileName, false);
            } else {
                getLogger().warning("Language file " + fileName + " not found! Using PL default.");
                messagesFile = new File(getDataFolder(), "messages_pl.yml");
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMsg(String path) {
        String msg = messagesConfig.getString(path);
        if (msg == null) return ChatColor.RED + "Missing message: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!getConfig().getString("punishment-mode").equalsIgnoreCase("ban")) {
            return;
        }

        UUID uuid = event.getUniqueId();
        String status = dataConfig.getString("status." + uuid, "alive");

        if (status.equals("dead")) {
            long reviveTime = dataConfig.getLong("reviveTime." + uuid, 0);
            long timeLeft = reviveTime - System.currentTimeMillis();

            if (timeLeft > 0) {
                Date expireDate = new Date(reviveTime);
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                String dateString = format.format(expireDate);
                String timeString = formatTime(timeLeft);

                String message = getMsg("kick-screen-layout")
                        .replace("{DATE}", dateString)
                        .replace("{TIME}", timeString)
                        .replace("\\n", "\n");

                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
            } else {
                dataConfig.set("status." + uuid, "alive");
                dataConfig.set("reviveTime." + uuid, null);
                saveData();
                event.allow();
            }
        }
    }

    private void setupGhostTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ghostTeam = scoreboard.getTeam("ReviveMeGhosts");
        if (ghostTeam == null) {
            ghostTeam = scoreboard.registerNewTeam("ReviveMeGhosts");
        }
        ghostTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        ghostTeam.setCanSeeFriendlyInvisibles(true);
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        int deaths = dataConfig.getInt("deaths." + uuid, 0) + 1;
        dataConfig.set("deaths." + uuid, deaths);
        dataConfig.set("status." + uuid, "dead");

        long minutes = getConfig().getInt("ban-duration-minutes", 180);
        long durationMillis = minutes * 60 * 1000L;
        long reviveTime = System.currentTimeMillis() + durationMillis;
        dataConfig.set("reviveTime." + uuid, reviveTime);
        saveData();

        String mode = getConfig().getString("punishment-mode");

        if (mode.equalsIgnoreCase("ban")) {
            long hours = minutes / 60;
            long mins = minutes % 60;
            String timeDisplay = (hours > 0 ? hours + "h " : "") + mins + "m";

            String kickMessage = getMsg("kick-message-layout")
                    .replace("{TIME}", timeDisplay)
                    .replace("\\n", "\n");

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.kickPlayer(kickMessage);
                }
            }.runTaskLater(this, 2L);

        } else {
            player.sendMessage(getMsg("ghost-mode-start"));
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyPunishmentMode(player);
                }
            }.runTaskLater(this, 1L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isDead(event.getPlayer())) {
            Bukkit.getScheduler().runTaskLater(this, () -> applyPunishmentMode(event.getPlayer()), 2L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (isDead(event.getPlayer())) {
            applyPunishmentMode(event.getPlayer());
        }
    }

    private void applyPunishmentMode(Player player) {
        String mode = getConfig().getString("punishment-mode");

        if (mode.equalsIgnoreCase("ban")) return;

        player.setHealth(20);
        player.setFoodLevel(20);

        if (mode.equalsIgnoreCase("spectator")) {
            enableSpectatorMode(player);
        } else {
            enableGhostMode(player);
        }
    }

    private void enableSpectatorMode(Player player) {
        List<Player> alivePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !isDead(p) && p != player)
                .collect(Collectors.toList());

        if (alivePlayers.isEmpty()) {
            long reviveTime = dataConfig.getLong("reviveTime." + player.getUniqueId(), 0);
            long timeLeft = reviveTime - System.currentTimeMillis();
            String timeString = (timeLeft > 0) ? formatTime(timeLeft) : "00:00:00";

            player.kickPlayer(getMsg("kick-spectator-no-players")
                    .replace("{TIME}", timeString)
                    .replace("\\n", "\n"));
            return;
        }

        player.getInventory().clear();

        player.setGameMode(GameMode.SPECTATOR);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        removeGhostEntity(player);

        if (!alivePlayers.isEmpty()) {
            Player target = alivePlayers.get(0);
            player.setSpectatorTarget(target);
            player.sendTitle("", ChatColor.GREEN + "Oglądasz: " + target.getName(), 10, 40, 10);
        }

        ItemStack comp = new ItemStack(Material.COMPASS);
        ItemMeta meta = comp.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Menu Spectatora (Prawy PM)");
        comp.setItemMeta(meta);

        player.getInventory().setItem(0, comp);
        player.getInventory().setItem(4, comp);
        player.getInventory().setItem(8, comp);

        openSpectatorGUI(player);
    }

    private void resetPlayerState(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setSpectatorTarget(null);
        }

        player.getInventory().clear();
        player.setItemOnCursor(null);
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);

        player.setGameMode(GameMode.SURVIVAL);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setAllowFlight(false);
        player.setFlying(false);
        if (ghostTeam.hasEntry(player.getName())) {
            ghostTeam.removeEntry(player.getName());
        }
        removeGhostEntity(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeGhostEntity(event.getPlayer());
    }

    private void enableGhostMode(Player player) {
        player.setGameMode(GameMode.ADVENTURE);

        fillGhostInventory(player);

        ghostTeam.addEntry(player.getName());
        spawnGhostEntity(player);

        boolean canFly = getConfig().getBoolean("ghost-fly", true);
        player.setAllowFlight(canFly);
        player.setFlying(canFly);

        player.getInventory().setHeldItemSlot(0);
    }

    private ItemStack getGhostGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private ItemStack getHauntItem() {
        ItemStack item = new ItemStack(Material.POLISHED_BLACKSTONE_BUTTON);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(getMsg("haunt-item-name"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillGhostInventory(Player player) {
        ItemStack glass = getGhostGlass();
        ItemStack haunt = getHauntItem();
        boolean hauntEnabled = getConfig().getBoolean("ghost-haunt-enabled", true);

        for (int i = 0; i < 36; i++) {
            if (i == 0) continue;
            if (i == 8 && hauntEnabled) {
                player.getInventory().setItem(i, haunt);
            } else {
                player.getInventory().setItem(i, glass);
            }
        }
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent e) {
    }

    private void disableGhostMode(Player player, boolean revive) {
        UUID entityId = ghostEntities.remove(player.getUniqueId());
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) entity.remove();
        }
        ghostTeam.removeEntry(player.getName());

        if (revive) {
            player.getInventory().clear();
            resetPlayerState(player);
        }
    }

    private boolean isDead(Player p) {
        String status = dataConfig.getString("status." + p.getUniqueId(), "alive");
        return status.equals("dead");
    }

    @EventHandler public void onMobTarget(EntityTargetLivingEntityEvent event) { if (event.getTarget() instanceof Player && isDead((Player) event.getTarget())) event.setCancelled(true); }
    @EventHandler public void onGhostPickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player && isDead((Player) e.getEntity())) e.setCancelled(true); }
    @EventHandler public void onGhostDrop(PlayerDropItemEvent e) { if (isDead(e.getPlayer())) e.setCancelled(true); }

    @EventHandler public void onInventoryDrag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player && isDead((Player) e.getWhoClicked())) e.setCancelled(true); }

    @EventHandler public void onHandSwap(PlayerSwapHandItemsEvent e) { if (isDead(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onGhostInteract(PlayerInteractEvent e) { if (isDead(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onGhostBreak(BlockBreakEvent e) { if (isDead(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onGhostPlace(BlockPlaceEvent e) { if (isDead(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onGhostDamage(EntityDamageByEntityEvent e) { if (e.getDamager() instanceof Player && isDead((Player) e.getDamager())) e.setCancelled(true); }
    @EventHandler public void onGhostReceiveDamage(EntityDamageEvent e) { if (e.getEntity() instanceof Player && isDead((Player) e.getEntity())) e.setCancelled(true); }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (isDead(player)) {
            String mode = getConfig().getString("punishment-mode");

            if (mode.equalsIgnoreCase("spectator")) {
                if (event.getItem() != null && event.getItem().getType() == Material.COMPASS) {
                    openSpectatorGUI(player);
                }
                return;
            }

            if (mode.equalsIgnoreCase("ghost")) {
                if (event.getItem() != null && event.getItem().getType() == Material.POLISHED_BLACKSTONE_BUTTON) {
                    if (event.getAction().name().contains("RIGHT")) {
                        handleHaunt(player);
                        player.getInventory().setHeldItemSlot(0);
                    }
                }
            }
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.hasBlock()) return;

        Block block = event.getClickedBlock();
        if (block.getType() != Material.CAULDRON && block.getType() != Material.WATER_CAULDRON) return;

        ItemStack item = event.getItem();
        TextDisplay bloodEntity = findBloodEntity(block);

        if (item != null && item.getType() == Material.POTION && bloodEntity == null) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null && meta.getBasePotionType() == PotionType.STRONG_HEALING) {
                if (!meta.hasDisplayName()) {
                    player.sendMessage(getMsg("potion-no-name"));
                    return;
                }
                String targetName = ChatColor.stripColor(meta.getDisplayName());

                UUID targetUUID = findPlayerUUID(targetName);
                if (targetUUID == null) {
                    player.sendMessage(ChatColor.RED + "Brak gracza w bazie.");
                    return;
                }

                if (!dataConfig.getString("status." + targetUUID, "alive").equals("dead")) {
                    player.sendMessage(getMsg("player-not-dead"));
                    return;
                }

                if (block.getType() == Material.WATER_CAULDRON) block.setType(Material.CAULDRON);

                spawnBloodLiquid(block, targetName);

                block.getWorld().playSound(block.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1, 1);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_LAVA_POP, 1, 1);
                player.sendMessage(getMsg("cauldron-fill").replace("{PLAYER}", targetName));
                player.getInventory().setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
                event.setCancelled(true);
            }
        }

        if (bloodEntity != null && item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
            String targetName = getTargetFromEntity(bloodEntity);

            if (targetName != null) {
                UUID targetUUID = findPlayerUUID(targetName);
                if (targetUUID != null) {
                    revivePlayer(Bukkit.getOfflinePlayer(targetUUID), block.getLocation().add(0.5, 1, 0.5));
                    bloodEntity.remove();
                    item.setAmount(item.getAmount() - 1);
                    block.getWorld().strikeLightningEffect(block.getLocation());
                    block.getWorld().playSound(block.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    block.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, block.getLocation().add(0.5, 1, 0.5), 100);
                    Bukkit.broadcastMessage(getMsg("revive-broadcast").replace("{PLAYER}", targetName));
                }
            }
        }
    }

    private void handleHaunt(Player player) {
        long cooldownTime = getConfig().getInt("ghost-haunt-cooldown", 45) * 1000L;
        long lastUse = hauntCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeLeft = (lastUse + cooldownTime) - System.currentTimeMillis();

        if (timeLeft > 0) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft);
            player.sendMessage(getMsg("haunt-cooldown").replace("{TIME}", String.valueOf(seconds)));
            return;
        }

        player.getWorld().playSound(player.getLocation(), Sound.AMBIENT_CAVE, 2.0f, 0.5f);
        player.sendMessage(getMsg("haunt-used"));
        hauntCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void revivePlayer(OfflinePlayer target, Location respawnLoc) {
        dataConfig.set("status." + target.getUniqueId(), "alive");
        dataConfig.set("reviveTime." + target.getUniqueId(), null);
        saveData();

        if (target.isOnline()) {
            Player p = target.getPlayer();
            resetPlayerState(p);
            p.teleport(respawnLoc);
            p.sendMessage(getMsg("revive-success"));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        }
    }

    @EventHandler public void onBlockBreak(BlockBreakEvent event) { cleanUpCauldron(event.getBlock()); }
    @EventHandler public void onEntityExplode(EntityExplodeEvent event) { event.blockList().forEach(this::cleanUpCauldron); }
    @EventHandler public void onBlockExplode(BlockExplodeEvent event) { event.blockList().forEach(this::cleanUpCauldron); }
    @EventHandler public void onPistonExtend(BlockPistonExtendEvent event) { event.getBlocks().forEach(this::cleanUpCauldron); }
    @EventHandler public void onPistonRetract(BlockPistonRetractEvent event) { event.getBlocks().forEach(this::cleanUpCauldron); }

    private void cleanUpCauldron(Block block) {
        if (block.getType() == Material.CAULDRON || block.getType() == Material.WATER_CAULDRON) {
            Collection<Entity> entities = block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 1.0, 1.0, 1.0);
            for (Entity e : entities) {
                if (e.getScoreboardTags().contains(BLOOD_TAG)) {
                    e.remove();
                    block.getWorld().spawnParticle(Particle.LARGE_SMOKE, block.getLocation().add(0.5, 0.5, 0.5), 10);
                }
            }
        }
    }

    private void spawnBloodLiquid(Block cauldron, String targetName) {
        Location spawnLoc = cauldron.getLocation().toCenterLocation();
        spawnLoc.setYaw(0);
        spawnLoc.setPitch(-90);

        TextDisplay display = (TextDisplay) cauldron.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
        display.addScoreboardTag(BLOOD_TAG);
        display.addScoreboardTag(TARGET_PREFIX + targetName);

        String nbtData = "{transformation:[3.8f,0.0f,0.0f,-0.1f, 0.0f,3.8f,0.0f,-0.56f, 0.0f,0.0f,3.8f,0.40f, 0.0f,0.0f,0.0f,1.0f], billboard:\"fixed\", background:0, text:{sprite:\"block/lava_still\", color:\"#530001\"}}";
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "data merge entity " + display.getUniqueId() + " " + nbtData);
    }

    private TextDisplay findBloodEntity(Block cauldron) {
        Collection<Entity> entities = cauldron.getWorld().getNearbyEntities(
                cauldron.getLocation().add(0.5, 0.5, 0.5), 1.0, 1.0, 1.0
        );
        for (Entity entity : entities) {
            if (entity instanceof TextDisplay && entity.getScoreboardTags().contains(BLOOD_TAG)) {
                return (TextDisplay) entity;
            }
        }
        return null;
    }

    private String getTargetFromEntity(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            if (tag.startsWith(TARGET_PREFIX)) {
                return tag.substring(TARGET_PREFIX.length());
            }
        }
        return null;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getScoreboardTags().contains(BLOOD_TAG)) event.setCancelled(true);
    }

    public void createDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}