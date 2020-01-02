package git.kmark43.skyblockvault;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class Vault {
    private UUID ownerUUID;
    private int availableSlots;
    private ItemStack[] contents;

    public Vault(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        availableSlots = SkyblockVaultPlugin.getInstance().getConfig().getInt("available_slots");
        contents = new ItemStack[availableSlots];
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void updateContents(ItemStack[] contents) {
        for (int i = 0; i < Math.min(contents.length, availableSlots); i++) {
            this.contents[i] = contents[i];
        }
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public ItemStack[] getContents() {
        return contents;
    }
}
