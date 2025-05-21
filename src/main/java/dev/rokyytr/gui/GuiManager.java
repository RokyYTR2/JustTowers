package dev.rokyytr.gui;

import dev.rokyytr.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiManager {
    private final GameManager gameManager;

    public GuiManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void openSetupGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Towers Setup");
        ItemStack countItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta countMeta = countItem.getItemMeta();
        assert countMeta != null;
        countMeta.setDisplayName(ChatColor.YELLOW + "Set Player Count: " + gameManager.getPlayerCountToStart());
        countItem.setItemMeta(countMeta);
        gui.setItem(4, countItem);
        player.openInventory(gui);
    }

    public void openVotingGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Towers Voting");
        int slot = 10;

        if (gameManager.isModeVotingEnabled()) {
            ItemStack lava = new ItemStack(Material.LAVA_BUCKET);
            ItemMeta lavaMeta = lava.getItemMeta();
            assert lavaMeta != null;
            lavaMeta.setDisplayName(ChatColor.RED + "Rising Lava");
            lava.setItemMeta(lavaMeta);

            ItemStack water = new ItemStack(Material.WATER_BUCKET);
            ItemMeta waterMeta = water.getItemMeta();
            assert waterMeta != null;
            waterMeta.setDisplayName(ChatColor.BLUE + "Rising Water");
            water.setItemMeta(waterMeta);

            ItemStack voidItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta voidMeta = voidItem.getItemMeta();
            assert voidMeta != null;
            voidMeta.setDisplayName(ChatColor.BLACK + "Void");
            voidItem.setItemMeta(voidMeta);

            gui.setItem(slot, lava);
            gui.setItem(slot + 2, water);
            gui.setItem(slot + 4, voidItem);
            slot += 9;
        }

        if (gameManager.isBiomeVotingEnabled()) {
            ItemStack overworld = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta overMeta = overworld.getItemMeta();
            assert overMeta != null;
            overMeta.setDisplayName(ChatColor.GREEN + "Overworld Biome");
            overworld.setItemMeta(overMeta);

            ItemStack nether = new ItemStack(Material.NETHERRACK);
            ItemMeta netherMeta = nether.getItemMeta();
            assert netherMeta != null;
            netherMeta.setDisplayName(ChatColor.DARK_RED + "Nether Biome");
            nether.setItemMeta(netherMeta);

            ItemStack end = new ItemStack(Material.END_STONE);
            ItemMeta endMeta = end.getItemMeta();
            assert endMeta != null;
            endMeta.setDisplayName(ChatColor.DARK_PURPLE + "End Biome");
            end.setItemMeta(endMeta);

            gui.setItem(slot, overworld);
            gui.setItem(slot + 2, nether);
            gui.setItem(slot + 4, end);
        }

        player.openInventory(gui);
    }

    public void updateSetupGui(Player player, Inventory gui) {
        ItemStack item = gui.getItem(4);
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.YELLOW + "Set Player Count: " + gameManager.getPlayerCountToStart());
            item.setItemMeta(meta);
            gui.setItem(4, item);
        }
    }
}