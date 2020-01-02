package git.kmark43.skyblockvault;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class VaultInventoryManager implements Listener {
    private Map<UUID, VaultInventoryGui> openVaults;
    private Map<UUID, Vault> playerVaultMap;

    public void enable() {
        openVaults = new HashMap<>();
        playerVaultMap = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadVaultAsync(player.getUniqueId(), (vault) -> {
                if (player.isOnline()) {
                    Bukkit.getLogger().info("Loaded " + player.getName() + "'s vault");
                    playerVaultMap.put(player.getUniqueId(), vault);
                }
            });
        }
    }

    public void disable() {
        for (UUID uuid : new ArrayList<>(openVaults.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            assert player != null;
            player.closeInventory();
        }

        for (UUID uuid : new ArrayList<>(playerVaultMap.keySet())) {
            saveVault(playerVaultMap.get(uuid));
        }

        openVaults = null;
        playerVaultMap = null;
    }

    public Vault getPlayerVault(Player player) {
        return playerVaultMap.get(player.getUniqueId());
    }

    public void openVault(Player opener, Vault vault) {
        VaultInventoryGui gui = new VaultInventoryGui(opener, vault);
        opener.openInventory(gui.getInventory());
        openVaults.put(opener.getUniqueId(), gui);
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent e) {
        if (!openVaults.containsKey(e.getWhoClicked().getUniqueId())) return;
        Player player = ((Player) e.getWhoClicked());
        openVaults.get(player.getUniqueId()).processClick(e);
    }

    @EventHandler
    public void onPlayerInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player)e.getPlayer();
        VaultInventoryGui gui = openVaults.remove(player.getUniqueId());
        if (gui != null) {
            gui.processClose();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadVaultAsync(e.getPlayer().getUniqueId(), (vault) -> {
            if (e.getPlayer().isOnline()) {
                Bukkit.getLogger().info("Loaded " + e.getPlayer().getName() + "'s vault");
                playerVaultMap.put(e.getPlayer().getUniqueId(), vault);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!playerVaultMap.containsKey(e.getPlayer().getUniqueId())) return;

        saveVaultAsync(e.getPlayer().getUniqueId(), () -> {
            Bukkit.getLogger().info("Saved " + e.getPlayer().getName() + "'s vault");
            if (!e.getPlayer().isOnline()) {
                playerVaultMap.remove(e.getPlayer().getUniqueId());
            }
        }, () -> {
            Bukkit.getLogger().warning("Failed to save " + e.getPlayer().getName() + "'s vault");
            if (!e.getPlayer().isOnline()) {
                playerVaultMap.remove(e.getPlayer().getUniqueId());
            }
        });
    }

    private void saveVaultAsync(UUID uuid, Runnable onSuccess, Runnable onFailure) {
        Vault vault = playerVaultMap.get(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(SkyblockVaultPlugin.getInstance(), () -> {
            if (saveVault(vault)) {
                Bukkit.getScheduler().runTask(SkyblockVaultPlugin.getInstance(), onSuccess);
            } else {
                Bukkit.getScheduler().runTask(SkyblockVaultPlugin.getInstance(), onFailure);
            }
        });
    }

    private void loadVaultAsync(UUID uuid, Consumer<Vault> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(SkyblockVaultPlugin.getInstance(), () -> {
            Vault vault = loadVault(uuid);
            Bukkit.getScheduler().runTask(SkyblockVaultPlugin.getInstance(), () -> {
                callback.accept(vault);
            });
        });
    }

    private boolean saveVault(Vault vault) {
        if (vault == null) return false;
        File file = getVaultFile(vault.getOwnerUUID());
        YamlConfiguration config = new YamlConfiguration();
        config.set("contents", vault.getContents());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Vault loadVault(UUID uuid) {
        File file = getVaultFile(uuid);
        Vault vault = new Vault(uuid);
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            @SuppressWarnings("unchecked")
            List<ItemStack> items = (List<ItemStack>)config.get("contents", new ArrayList<ItemStack>());
            assert items != null;
            int availableSlots = SkyblockVaultPlugin.getInstance().getConfig().getInt("available_slots");
            vault.updateContents(items.toArray(new ItemStack[0]));
        }
        return vault;
    }

    private File getVaultFolder() {
        return new File(SkyblockVaultPlugin.getInstance().getDataFolder(), "vaults");
    }

    private File getVaultFile(UUID uuid) {
        return new File(getVaultFolder(), uuid.toString() + ".yml");
    }
}
