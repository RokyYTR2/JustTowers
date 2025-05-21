package dev.rokyytr.listeners;

import dev.rokyytr.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {
    private final GameManager gameManager;

    public EventListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        gameManager.addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (title.equals(ChatColor.DARK_PURPLE + "Towers Setup")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() == Material.PLAYER_HEAD) {
                gameManager.updatePlayerCount(event.isLeftClick());
                gameManager.getGuiManager().updateSetupGui(player, event.getInventory());
            }
        } else if (title.equals(ChatColor.DARK_PURPLE + "Towers Voting")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getItemMeta() != null) {
                gameManager.handleVote(player, item.getItemMeta().getDisplayName());
            }
        }
    }
}