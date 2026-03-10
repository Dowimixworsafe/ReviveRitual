package pl.Dowimixworsafe.reviveMe;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.Dowimixworsafe.reviveMe.commands.ReviveMeCommand;
import pl.Dowimixworsafe.reviveMe.listeners.*;
import pl.Dowimixworsafe.reviveMe.managers.*;

public final class ReviveMe extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private RevivalManager revivalManager;
    private GhostManager ghostManager;
    private PunishmentManager punishmentManager;
    private TombstoneManager tombstoneManager;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.tombstoneManager = new TombstoneManager(this, dataManager);
        this.ghostManager = new GhostManager(this, dataManager, configManager);
        this.revivalManager = new RevivalManager(this, dataManager, configManager);
        this.punishmentManager = new PunishmentManager(this, configManager, dataManager, ghostManager);

        getCommand("reviveme")
                .setExecutor(new ReviveMeCommand(this, configManager, dataManager, revivalManager, punishmentManager));

        getServer().getPluginManager().registerEvents(new GhostListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorListener(this), this);
        getServer().getPluginManager().registerEvents(new CauldronListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);

        ghostManager.startGhostTask();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (dataManager.isDead(p)) {
                punishmentManager.applyPunishmentMode(p);
            }
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null)
            dataManager.saveData();
        if (ghostManager != null)
            ghostManager.removeAllGhosts();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public RevivalManager getRevivalManager() {
        return revivalManager;
    }

    public GhostManager getGhostManager() {
        return ghostManager;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public TombstoneManager getTombstoneManager() {
        return tombstoneManager;
    }
}