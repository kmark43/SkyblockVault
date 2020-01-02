package git.kmark43.skyblockvault;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyblockVaultPlugin extends JavaPlugin {
    private static SkyblockVaultPlugin instance;
    private VaultInventoryManager vaultInventoryManager;

    public static SkyblockVaultPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;
        registerCommands();
        vaultInventoryManager = new VaultInventoryManager();
        Bukkit.getPluginManager().registerEvents(vaultInventoryManager, this);
        vaultInventoryManager.enable();
    }

    private void registerCommands() {
        getCommand("vault").setExecutor(new VaultCommandExecutor());
    }

    @Override
    public void onDisable() {
        vaultInventoryManager.disable();
        vaultInventoryManager = null;
        instance = null;
    }

    public VaultInventoryManager getVaultInventoryManager() {
        return vaultInventoryManager;
    }
}
