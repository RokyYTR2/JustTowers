package dev.rokyytr.commands;

import dev.rokyytr.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TowersCommand implements CommandExecutor {
    private final GameManager gameManager;

    public TowersCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command is for players only!");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /towers <setup|tp|join>");
            return true;
        }
        if (args[0].equalsIgnoreCase("setup")) {
            if (!player.hasPermission("towers.setup")) {
                player.sendMessage(ChatColor.RED + "No permission!");
                return true;
            }
            gameManager.addPlayerToLobby(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("tp")) {
            if (!player.hasPermission("towers.tp")) {
                player.sendMessage(ChatColor.RED + "No permission!");
                return true;
            }
            if (gameManager.getGameWorld() == null) {
                player.sendMessage(ChatColor.RED + "No Towers world is active! Use /towers setup first.");
                return true;
            }
            if (gameManager.getTowerLocations().isEmpty()) {
                player.sendMessage(ChatColor.RED + "No towers have been generated yet.");
                return true;
            }
            player.teleport(gameManager.getTowerLocations().get(0).clone().add(0.5, 1, 0.5));
            player.sendMessage(ChatColor.GREEN + "Teleported to a Towers game tower!");
            return true;
        }
        if (args[0].equalsIgnoreCase("join")) {
            gameManager.addPlayerToLobby(player);
            return true;
        }
        player.sendMessage(ChatColor.RED + "Unknown subcommand! Use /towers <setup|tp|join>");
        return true;
    }
}