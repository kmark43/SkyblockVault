package git.kmark43.skyblockvault;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class VaultCommandExecutor implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = SkyblockVaultPlugin.getInstance().getConfig();
        if (!(sender instanceof Player)) {
            sender.sendMessage(getConfigMessage(config, "must_be_player"));
            return true;
        }
        Player player = (Player) sender;
        VaultInventoryManager vaultManager = SkyblockVaultPlugin.getInstance().getVaultInventoryManager();
        Vault vault = vaultManager.getPlayerVault(player);
        if (vault == null) {
            sender.sendMessage(getConfigMessage(config, "vault_not_loaded"));
        } else {
            vaultManager.openVault(player, vault);
        }
        return true;
    }

    private String getConfigMessage(FileConfiguration configuration, String property) {
        return ChatColor.translateAlternateColorCodes('&', configuration.getString("messages." + property, ""));
    }
}
