package git.kmark43.skyblockvault;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class VaultInventoryGui {
    private int page;
    private Inventory inventory;
    private Map<Integer, ItemClickHandler> handlerMap;
    private int itemsPerPage;
    private Vault vault;

    public VaultInventoryGui(InventoryHolder owner, Vault vault) {
        int availableSlots = vault.getContents().length;
        int rows = (int)Math.min(6, Math.max(1, Math.ceil(availableSlots / 9.0)));
        this.handlerMap = new HashMap<>();
        this.vault = vault;

        String title = SkyblockVaultPlugin.getInstance().getConfig().getString("vault_title");
        inventory = Bukkit.createInventory(owner, 9 * rows, title);
        if (availableSlots <= 9 * 6) {
            for (int i = 0; i < availableSlots; i++) {
                inventory.setItem(i, vault.getContents()[i]);
            }
            for (int i = availableSlots; i < 9 * rows; i++) {
                addClickableItem(i, getUnavailableStack(), e -> e.setCancelled(true));
            }
            itemsPerPage = availableSlots;
        } else {
            itemsPerPage = 5 * 9;
            setPage(0);
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Vault getVault() {
        return vault;
    }

    public void addClickableItem(int slot, ItemStack stack, ItemClickHandler clickHandler) {
        inventory.setItem(slot, stack);
        handlerMap.put(slot, clickHandler);
    }

    public void processClick(InventoryClickEvent e) {
        if (handlerMap.containsKey(e.getRawSlot())) {
            handlerMap.get(e.getRawSlot()).onItemClick(e);
        }
    }

    private void saveCurrentPage() {
        int start = page * itemsPerPage;

        for (int i = 0; i < itemsPerPage && i + start < vault.getContents().length; i++) {
            vault.getContents()[i + start] = inventory.getItem(i);
        }
    }

    private void nextPage() {
        if (page < getNumPages() - 1) {
            saveCurrentPage();
            setPage(page + 1);
        }
    }

    private void prevPage() {
        if (page > 0) {
            saveCurrentPage();
            setPage(page - 1);
        }
    }

    private void setPage(int page) {
        this.page = page;
        int start = itemsPerPage * page;
        int i;
        for (i = 0; i < itemsPerPage && i + start < vault.getContents().length; i++) {
            inventory.setItem(i, vault.getContents()[i + start]);
        }
        for (; i < itemsPerPage; i++) {
            inventory.setItem(i, getUnavailableStack());
        }
        updatePageControls();
    }

    private void updatePageControls() {
        for (int i = 5 * 9; i < 6 * 9; i++) {
            addClickableItem(i, getUnavailableStack(), e -> e.setCancelled(true));
        }
        if (page != 0) {
            addClickableItem(5 * 9, getPrevStack(), e -> {
                e.setCancelled(true);
                prevPage();
            });
        }
        if (page != getNumPages() - 1) {
            addClickableItem(5 * 9 + 8, getNextStack(), e -> {
                e.setCancelled(true);
                nextPage();
            });
        }
        addClickableItem(5 * 9 + 4, getPageStack(), e -> e.setCancelled(true));
    }

    private ItemStack getPrevStack() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("" + ChatColor.RED + "Previous Page");
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack getNextStack() {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("" + ChatColor.GREEN + "Next Page");
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack getPageStack() {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("" + ChatColor.GRAY + "Page " + ChatColor.GRAY + (page + 1) + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + getNumPages());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack getUnavailableStack() {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "Unavailable");
        stack.setItemMeta(meta);
        return stack;
    }

    public int getPage() {
        return page;
    }

    public int getNumPages() {
        return (int)Math.max(1, Math.ceil(vault.getContents().length / (9.0 * 6)));
    }

    public void processClose() {
        saveCurrentPage();
    }
}
