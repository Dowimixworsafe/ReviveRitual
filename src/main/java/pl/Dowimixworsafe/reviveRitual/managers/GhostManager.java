package pl.Dowimixworsafe.reviveRitual.managers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;
import pl.Dowimixworsafe.reviveRitual.utils.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GhostManager {

    private final ReviveRitual plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    public final Map<UUID, UUID> ghostEntities = new HashMap<>();
    private Team ghostTeam;

    public GhostManager(ReviveRitual plugin, DataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
        setupGhostTeam();
    }

    private void setupGhostTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        ghostTeam = scoreboard.getTeam("ReviveRitualGhosts");
        if (ghostTeam == null) {
            ghostTeam = scoreboard.registerNewTeam("ReviveRitualGhosts");
        }
        ghostTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        ghostTeam.setCanSeeFriendlyInvisibles(true);
    }

    public void startGhostTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (dataManager.getData().getConfigurationSection("status") == null) return;

                List<Player> alivePlayers = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !dataManager.isDead(p))
                        .collect(Collectors.toList());

                for (String uuidStr : dataManager.getData().getConfigurationSection("status").getKeys(false)) {
                    if (!dataManager.getData().getString("status." + uuidStr).equals("dead")) continue;

                    UUID uuid = UUID.fromString(uuidStr);
                    Player player = Bukkit.getPlayer(uuid);

                    if (player == null || !player.isOnline()) continue;

                    long reviveTime = dataManager.getData().getLong("reviveTime." + uuid, 0);
                    long timeLeft = reviveTime - System.currentTimeMillis();

                    if (timeLeft <= 0) {
                        Location respawnLoc = player.getRespawnLocation();
                        if (respawnLoc == null) {
                            respawnLoc = player.getWorld().getSpawnLocation();
                        }
                        plugin.getRevivalManager().revivePlayer(player, respawnLoc);

                        continue;
                    }

                    String timeString = TimeUtils.formatTime(timeLeft);
                    String actionMsg = configManager.getMsg("action-bar-timer").replace("{TIME}", timeString);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionMsg));

                    String mode = plugin.getConfig().getString("punishment-mode");

                    if (mode.equalsIgnoreCase("ghost")) {
                        handleGhostEntity(player);
                    }
                    else if (mode.equalsIgnoreCase("spectator")) {
                        if (alivePlayers.isEmpty()) {
                            player.kickPlayer(configManager.getMsg("kick-spectator-no-players")
                                    .replace("{TIME}", timeString)
                                    .replace("\\n", "\n"));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleGhostEntity(Player player) {
        UUID entityUUID = ghostEntities.get(player.getUniqueId());
        Entity ghostEntity = (entityUUID != null) ? Bukkit.getEntity(entityUUID) : null;

        if (ghostEntity == null || ghostEntity.isDead()) {
            spawnGhostEntity(player);
            return;
        }

        boolean canFly = plugin.getConfig().getBoolean("ghost-fly", true);
        Location loc = player.getLocation();

        if (canFly) ghostEntity.teleport(loc.add(0, 0.5, 0));
        else ghostEntity.teleport(loc);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));
    }

    public void spawnGhostEntity(Player player) {
        removeGhostEntity(player);

        String mobTypeStr = plugin.getConfig().getString("ghost-mob-type", "ALLAY").toUpperCase();
        EntityType type;
        try {
            type = EntityType.valueOf(mobTypeStr);
        } catch (IllegalArgumentException e) {
            type = EntityType.ALLAY;
        }

        Entity ghost = player.getWorld().spawnEntity(player.getLocation(), type);

        if (ghost instanceof LivingEntity) {
            ((LivingEntity) ghost).setAI(false);
            ((LivingEntity) ghost).setCollidable(false);
        }

        ghost.setInvulnerable(true);
        ghost.setSilent(true);
        ghost.setFireTicks(0);
        ghost.setVisualFire(false);

        ghost.setGlowing(plugin.getConfig().getBoolean("ghost-glowing", true));

        boolean nametag = plugin.getConfig().getBoolean("ghost-nametag-visible", true);
        if (nametag) {
            ghost.setCustomName(ChatColor.YELLOW + player.getName());
            ghost.setCustomNameVisible(true);
        } else {
            ghost.setCustomNameVisible(false);
        }

        if (ghost instanceof Allay) ((Allay) ghost).setCanPickupItems(false);

        player.hideEntity(plugin, ghost);
        ghostTeam.addEntry(ghost.getUniqueId().toString());
        ghostEntities.put(player.getUniqueId(), ghost.getUniqueId());
    }

    public void removeGhostEntity(Player player) {
        if (ghostEntities.containsKey(player.getUniqueId())) {
            UUID entityId = ghostEntities.remove(player.getUniqueId());
            Entity e = Bukkit.getEntity(entityId);
            if (e != null) e.remove();
        }
    }

    public void removeAllGhosts() {
        for (UUID entityId : ghostEntities.values()) {
            Entity e = Bukkit.getEntity(entityId);
            if (e != null) e.remove();
        }
        ghostEntities.clear();
    }
}