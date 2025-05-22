package dev.rokyytr.gui;

import dev.rokyytr.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class GuiManager {
    private final GameManager gameManager;

    public GuiManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void openSetupGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Towers Setup");

        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.setDisplayName(ChatColor.RESET + "");
            border.setItemMeta(borderMeta);
        }

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
        }

        ItemStack decrease = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta decreaseMeta = decrease.getItemMeta();
        if (decreaseMeta != null) {
            decreaseMeta.setDisplayName(ChatColor.RED + "Decrease Player Count");
            decrease.setItemMeta(decreaseMeta);
        }
        gui.setItem(2, decrease);

        ItemStack countItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta countMeta = countItem.getItemMeta();
        if (countMeta != null) {
            countMeta.setDisplayName(ChatColor.YELLOW + "Player Count: " + gameManager.getPlayerCountToStart());
            countMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Current: " + gameManager.getPlayerCountToStart(),
                    ChatColor.GRAY + "Min: 2, Max: 16"
            ));
            countItem.setItemMeta(countMeta);
        }
        gui.setItem(4, countItem);

        ItemStack increase = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta increaseMeta = increase.getItemMeta();
        if (increaseMeta != null) {
            increaseMeta.setDisplayName(ChatColor.GREEN + "Increase Player Count");
            increase.setItemMeta(increaseMeta);
        }
        gui.setItem(6, increase);

        player.openInventory(gui);
    }

    public void openVotingGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + "Towers Voting");

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.RESET + "");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, filler);
            gui.setItem(i + 27, filler);
        }

        if (gameManager.isModeVotingEnabled()) {
            ItemStack lava = new ItemStack(Material.LAVA_BUCKET);
            ItemMeta lavaMeta = lava.getItemMeta();
            if (lavaMeta != null) {
                lavaMeta.setDisplayName(ChatColor.RED + "Rising Lava");
                lavaMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Lava rises from below!"));
                lava.setItemMeta(lavaMeta);
            }
            gui.setItem(12, lava);

            ItemStack water = new ItemStack(Material.WATER_BUCKET);
            ItemMeta waterMeta = water.getItemMeta();
            if (waterMeta != null) {
                waterMeta.setDisplayName(ChatColor.BLUE + "Rising Water");
                waterMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Water rises and damages!"));
                water.setItemMeta(waterMeta);
            }
            gui.setItem(13, water);

            ItemStack voidItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta voidMeta = voidItem.getItemMeta();
            if (voidMeta != null) {
                voidMeta.setDisplayName(ChatColor.BLACK + "Void");
                voidMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Fall into the void and die!"));
                voidItem.setItemMeta(voidMeta);
            }
            gui.setItem(14, voidItem);

            ItemStack border = new ItemStack(Material.BARRIER);
            ItemMeta borderMeta = border.getItemMeta();
            if (borderMeta != null) {
                borderMeta.setDisplayName(ChatColor.DARK_GRAY + "Shrinking Border");
                borderMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Border closes in over time!"));
                border.setItemMeta(borderMeta);
            }
            gui.setItem(15, border);
        }

        if (gameManager.isBiomeVotingEnabled()) {
            ItemStack overworld = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta overMeta = overworld.getItemMeta();
            if (overMeta != null) {
                overMeta.setDisplayName(ChatColor.GREEN + "Overworld Biome");
                overMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Overworld dimension."));
                overworld.setItemMeta(overMeta);
            }
            gui.setItem(21, overworld);

            ItemStack nether = new ItemStack(Material.NETHERRACK);
            ItemMeta netherMeta = nether.getItemMeta();
            if (netherMeta != null) {
                netherMeta.setDisplayName(ChatColor.DARK_RED + "Nether Biome");
                netherMeta.setLore(Collections.singletonList(ChatColor.GRAY + "Nether dimension."));
                nether.setItemMeta(netherMeta);
            }
            gui.setItem(22, nether);

            ItemStack end = new ItemStack(Material.END_STONE);
            ItemMeta endMeta = end.getItemMeta();
            if (endMeta != null) {
                endMeta.setDisplayName(ChatColor.DARK_PURPLE + "End Biome");
                endMeta.setLore(Collections.singletonList(ChatColor.GRAY + "End dimension."));
                end.setItemMeta(endMeta);
            }
            gui.setItem(23, end);
        }

        player.openInventory(gui);

        giveVotingItem(player);
    }

    public void giveVotingItem(Player player) {
        player.getInventory().clear();

        ItemStack votingItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta votingMeta = votingItem.getItemMeta();
        if (votingMeta != null) {
            votingMeta.setDisplayName(ChatColor.GOLD + "Vote for Game Mode & Biome");
            votingMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right-click to open voting menu!",
                    ChatColor.YELLOW + "Choose your preferred game mode and biome"
            ));
            votingItem.setItemMeta(votingMeta);
        }
        player.getInventory().setItem(4, votingItem);
    }

    public void updateSetupGui(Player player, Inventory gui) {
        ItemStack item = gui.getItem(4);
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + "Player Count: " + gameManager.getPlayerCountToStart());
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Current: " + gameManager.getPlayerCountToStart(),
                        ChatColor.GRAY + "Min: 2, Max: 16"
                ));
                item.setItemMeta(meta);
                gui.setItem(4, item);
            }
        }
    }
}