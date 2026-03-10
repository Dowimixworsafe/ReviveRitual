package pl.Dowimixworsafe.reviveRitual.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {

    private final ReviveRitual plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(ReviveRitual plugin) {
        this.plugin = plugin;
        createDataFile();
    }

    public void createDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public FileConfiguration getData() {
        return dataConfig;
    }

    public boolean isDead(Player p) {
        String status = dataConfig.getString("status." + p.getUniqueId(), "alive");
        return status.equals("dead");
    }

    public boolean isDead(UUID uuid) {
        String status = dataConfig.getString("status." + uuid, "alive");
        return status.equals("dead");
    }

    public void setStatus(UUID uuid, String status) {
        dataConfig.set("status." + uuid, status);
        saveData();
    }
}