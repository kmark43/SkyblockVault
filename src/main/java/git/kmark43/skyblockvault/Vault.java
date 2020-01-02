package git.kmark43.skyblockvault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Vault {
    private static int rows = 4;
    private static int availableSlots = 32;

    private ItemStack[] contents;

    public Vault() {
        contents = new ItemStack[availableSlots];
        availableSlots = SkyblockVaultPlugin.getInstance().getConfig().getInt("available_slots");
        rows = (int)Math.min(6, Math.max(1, Math.ceil(availableSlots / 9.0)));
    }

    public Inventory getInventory(InventoryHolder owner) {
        String title = SkyblockVaultPlugin.getInstance().getConfig().getString("vault_title");
        Inventory inventory = Bukkit.createInventory(owner, 9 * rows, title);
        for (int i = 0; i < availableSlots; i++) {
            inventory.setItem(i, contents[i]);
        }
        for (int i = availableSlots; i < 9 * rows; i++) {
            ItemStack stack = new ItemStack(Material.BARRIER);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "Unavailable");
            stack.setItemMeta(meta);
            inventory.setItem(i, stack);
        }
        return inventory;
    }

    public void updateContents(ItemStack[] contents) {
        for (int i = 0; i < Math.min(contents.length, availableSlots); i++) {
            this.contents[i] = contents[i];
        }
    }

    public ItemStack[] getContents() {
        return contents;
    }
}
