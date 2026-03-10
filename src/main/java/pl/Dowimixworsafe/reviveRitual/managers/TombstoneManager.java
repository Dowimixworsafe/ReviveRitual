package pl.Dowimixworsafe.reviveRitual.managers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.Dowimixworsafe.reviveRitual.ReviveRitual;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TombstoneManager {

    private final ReviveRitual plugin;
    private final DataManager dataManager;

    public TombstoneManager(ReviveRitual plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public void createGrave(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        String graveId = UUID.randomUUID().toString().substring(0, 8);

        String basePath = "grave." + uuid + "." + graveId;
        dataManager.getData().set(basePath + ".items", null);
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && !contents[i].getType().isAir()) {
                dataManager.getData().set(basePath + ".items." + i, contents[i]);
            }
        }
        dataManager.getData().set(basePath + ".exp", player.getTotalExperience());
        dataManager.getData().set(basePath + ".level", player.getLevel());
        dataManager.getData().set(basePath + ".expFloat", player.getExp());

        dataManager.getData().set(basePath + ".location", loc);
        dataManager.saveData();

        spawnGraveVisuals(player, loc, graveId);
    }

    public void spawnGraveVisuals(Player player, Location loc, String graveId) {

        loc.setPitch(0);
        loc = loc.getBlock().getLocation().add(0.5, 0, 0.5);

        UUID uuid = player.getUniqueId();

        BlockDisplay base = (BlockDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.BLOCK_DISPLAY);
        base.setBlock(Bukkit.createBlockData(Material.STONE));
        base.setTransformation(new Transformation(
                new Vector3f(-0.4f, 0f, -0.1f),
                new AxisAngle4f(),
                new Vector3f(0.8f, 1.2f, 0.2f),
                new AxisAngle4f()));

        ItemDisplay head = (ItemDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.ITEM_DISPLAY);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            skull.setItemMeta(meta);
        }
        head.setItemStack(skull);

        head.setTransformation(new Transformation(
                new Vector3f(0f, 0.8f, 0.105f),
                new AxisAngle4f((float) Math.toRadians(180), 0f, 1f, 0f),
                new Vector3f(0.5f, 0.5f, 0.05f),
                new AxisAngle4f()));

        TextDisplay text = (TextDisplay) loc.getWorld().spawnEntity(loc.clone(), EntityType.TEXT_DISPLAY);
        String dateStr = new SimpleDateFormat("dd.MM.yyyy").format(new Date());

        text.setText(player.getName() + "\n" + dateStr);
        text.setLineWidth(200);
        text.setAlignment(TextDisplay.TextAlignment.CENTER);

        text.setBillboard(Display.Billboard.FIXED);
        text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        text.setShadowed(false);

        text.setTransformation(new Transformation(
                new Vector3f(0f, 0.35f, 0.11f),
                new AxisAngle4f(),
                new Vector3f(0.25f, 0.25f, 0.25f),
                new AxisAngle4f()));

        Interaction interaction = (Interaction) loc.getWorld().spawnEntity(loc, EntityType.INTERACTION);
        interaction.setInteractionWidth(0.8f);
        interaction.setInteractionHeight(1.2f);

        String tag = "grave_" + uuid.toString() + "_" + graveId;
        base.addScoreboardTag(tag);
        head.addScoreboardTag(tag);
        text.addScoreboardTag(tag);
        interaction.addScoreboardTag(tag);
    }

    public void tryLootGrave(Player player, Interaction interaction) {
        if (plugin.getDataManager().isDead(player)) {
            player.sendMessage(plugin.getConfigManager().getMsg("grave-cannot-loot-dead"));
            return;
        }

        UUID uuid = player.getUniqueId();
        String myPrefix = "grave_" + uuid.toString();
        String foundGraveId = null;
        boolean isOtherPlayerGrave = false;

        for (String tag : interaction.getScoreboardTags()) {
            if (tag.startsWith("grave_")) {
                if (tag.equals(myPrefix)) {
                    foundGraveId = "legacy";
                } else if (tag.startsWith(myPrefix + "_")) {
                    foundGraveId = tag.substring((myPrefix + "_").length());
                } else {
                    isOtherPlayerGrave = true;
                }
            }
        }

        if (foundGraveId != null) {
            restoreItems(player, foundGraveId);
            removeGrave(uuid, foundGraveId);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            player.sendMessage(plugin.getConfigManager().getMsg("grave-looted"));
        } else if (isOtherPlayerGrave) {
            player.sendMessage(plugin.getConfigManager().getMsg("grave-not-yours"));
        }
    }

    private void restoreItems(Player player, String graveId) {
        UUID uuid = player.getUniqueId();
        String basePath = graveId.equals("legacy") ? "grave" : ("grave." + uuid + "." + graveId);
        String itemsPath = basePath + ".items" + (graveId.equals("legacy") ? "." + uuid : "");

        if (dataManager.getData().contains(itemsPath)) {
            org.bukkit.configuration.ConfigurationSection itemsSection = dataManager.getData()
                    .getConfigurationSection(itemsPath);
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        ItemStack item = itemsSection.getItemStack(key);
                        if (item != null) {
                            ItemStack existing = player.getInventory().getItem(slot);
                            if (existing == null || existing.getType().isAir()) {
                                player.getInventory().setItem(slot, item);
                            } else {
                                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                                for (ItemStack left : leftover.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                                }
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        String expPath = basePath + ".exp" + (graveId.equals("legacy") ? "." + uuid : "");
        String levelPath = basePath + ".level" + (graveId.equals("legacy") ? "." + uuid : "");
        String expFloatPath = basePath + ".expFloat" + (graveId.equals("legacy") ? "." + uuid : "");

        player.setTotalExperience(dataManager.getData().getInt(expPath, 0));
        player.setLevel(dataManager.getData().getInt(levelPath, 0));
        player.setExp((float) dataManager.getData().getDouble(expFloatPath, 0.0));
    }

    public void removeGrave(UUID uuid, String graveId) {
        removeGraveVisuals(uuid, graveId);

        if (graveId.equals("legacy")) {
            dataManager.getData().set("grave.items." + uuid, null);
            dataManager.getData().set("grave.exp." + uuid, null);
            dataManager.getData().set("grave.level." + uuid, null);
            dataManager.getData().set("grave.expFloat." + uuid, null);
            dataManager.getData().set("grave.location." + uuid, null);
        } else {
            dataManager.getData().set("grave." + uuid + "." + graveId, null);
        }
        dataManager.saveData();
    }

    public void removeGraveVisuals(UUID uuid, String graveId) {
        String locPath = graveId.equals("legacy") ? "grave.location." + uuid
                : "grave." + uuid + "." + graveId + ".location";
        Location loc = dataManager.getData().getLocation(locPath);
        if (loc != null && loc.getWorld() != null) {
            String targetTag = graveId.equals("legacy") ? "grave_" + uuid.toString()
                    : "grave_" + uuid.toString() + "_" + graveId;
            for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                if (e.getScoreboardTags().contains(targetTag)) {
                    if (e instanceof Interaction) {
                        e.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, e.getLocation(), 20, 0.5, 0.5, 0.5,
                                0.01);
                    }
                    e.remove();
                }
            }
        }
    }
}
