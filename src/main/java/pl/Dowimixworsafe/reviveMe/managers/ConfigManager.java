package pl.Dowimixworsafe.reviveMe.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.Dowimixworsafe.reviveMe.ReviveMe;

import java.io.File;

public class ConfigManager {

    private final ReviveMe plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    public ConfigManager(ReviveMe plugin) {
        this.plugin = plugin;
        createMessagesFile();
    }

    public void createMessagesFile() {
        if (!new File(plugin.getDataFolder(), "messages_pl.yml").exists()) {
            plugin.saveResource("messages_pl.yml", false);
        }
        if (!new File(plugin.getDataFolder(), "messages_en.yml").exists()) {
            plugin.saveResource("messages_en.yml", false);
        }

        String lang = plugin.getConfig().getString("language", "pl");
        String fileName = "messages_" + lang + ".yml";

        messagesFile = new File(plugin.getDataFolder(), fileName);

        if (!messagesFile.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                plugin.getLogger().warning("Language file " + fileName + " not found! Using PL default.");
                messagesFile = new File(plugin.getDataFolder(), "messages_pl.yml");
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMsg(String path) {
        String msg = messagesConfig.getString(path);
        if (msg == null) return ChatColor.RED + "Missing message: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void reload() {
        createMessagesFile();
    }
}