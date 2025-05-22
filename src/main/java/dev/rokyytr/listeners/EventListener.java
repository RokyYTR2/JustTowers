package dev.rokyytr.listeners;

import dev.rokyytr.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

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
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameManager.getGameWorld() != null && event.getPlayer().getWorld().equals(gameManager.getGameWorld())) {
            if (!gameManager.isGameRunning()) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', gameManager.getPlugin().getConfig().getString("general.prefix", "&b[Towers] ") + "&cYou cannot break blocks during the lobby phase!"));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.equals(ChatColor.DARK_PURPLE + "Towers Setup")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR) {
                if (item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
                    gameManager.updatePlayerCount(true);
                    gameManager.getGuiManager().updateSetupGui(player, event.getInventory());
                } else if (item.getType() == Material.RED_STAINED_GLASS_PANE) {
                    gameManager.updatePlayerCount(false);
                    gameManager.getGuiManager().updateSetupGui(player, event.getInventory());
                }
            }
        } else if (title.equals(ChatColor.DARK_PURPLE + "Towers Voting")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.AIR && item.hasItemMeta() && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                String displayName = Objects.requireNonNull(item.getItemMeta()).getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    gameManager.handleVote(player, displayName);
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (gameManager.getGameWorld() != null && player.getWorld().equals(gameManager.getGameWorld())) {
            if (!gameManager.isGameRunning()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', gameManager.getPlugin().getConfig().getString("general.prefix", "&b[Towers] ") + "&cYou cannot drop items during the lobby phase!"));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
                String displayName = Objects.requireNonNull(item.getItemMeta()).getDisplayName();
                if (displayName != null && (displayName.equals(ChatColor.GOLD + "Vote for Game Mode & Biome") ||
                        displayName.equals(ChatColor.GOLD + "Vote for Game Settings"))) {
                    event.setCancelled(true);
                    if (gameManager.getGameWorld() != null && player.getWorld().equals(gameManager.getGameWorld()) && !gameManager.isGameRunning()) {
                        gameManager.getGuiManager().openVotingGui(player);
                    }
                }
            }
        }
    }
}