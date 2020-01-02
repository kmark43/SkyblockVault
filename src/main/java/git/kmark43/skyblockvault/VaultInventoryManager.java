package git.kmark43.skyblockvault;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
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

public class VaultInventoryManager implements Listener {
    private Set<UUID> playersWithOpenVaults;
    private Map<UUID, Vault> playerVaultMap;

    public void enable() {
        playersWithOpenVaults = new HashSet<>();
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
        for (UUID uuid : new ArrayList<>(playersWithOpenVaults)) {
            Player player = Bukkit.getPlayer(uuid);
            assert player != null;
            player.closeInventory();
        }

        for (UUID uuid : new ArrayList<>(playerVaultMap.keySet())) {
            saveVault(uuid, playerVaultMap.get(uuid));
        }

        playersWithOpenVaults = null;
        playerVaultMap = null;
    }

    public Vault getPlayerVault(Player player) {
        return playerVaultMap.get(player.getUniqueId());
    }

    public void openVault(Player opener, Vault vault) {
        Inventory inventory = vault.getInventory(opener);
        opener.openInventory(inventory);
        playersWithOpenVaults.add(opener.getUniqueId());
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent e) {
        if (playersWithOpenVaults.contains(e.getWhoClicked().getUniqueId())) {
            if (e.getRawSlot() >= 32 && e.getRawSlot() < 36) {
                e.setCancelled(true);
                // TODO doesn't prevent shift clicking barriers in to chest
            }
        }
    }

    @EventHandler
    public void onPlayerInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player)e.getPlayer();
        playersWithOpenVaults.remove(player.getUniqueId());
        getPlayerVault(player).updateContents(e.getInventory().getContents());
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
            if (saveVault(uuid, vault)) {
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

    private boolean saveVault(UUID uuid, Vault vault) {
        if (vault == null) return false;
        File file = getVaultFile(uuid);
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
        Vault vault = new Vault();
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            @SuppressWarnings("unchecked")
            List<ItemStack> items = (List<ItemStack>)config.get("contents", new ArrayList<ItemStack>());
            assert items != null;
            vault.updateContents(items.toArray(new ItemStack[32]));
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
