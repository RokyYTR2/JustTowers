package dev.rokyytr.commands;

import dev.rokyytr.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
            player.sendMessage(ChatColor.RED + "Usage: /towers <setup|tp>");
            return true;
        }
        if (args[0].equalsIgnoreCase("setup")) {
            if (!player.hasPermission("towers.setup")) {
                player.sendMessage(ChatColor.RED + "No permission!");
                return true;
            }
            gameManager.setupGame(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("join")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command is for players only!");
                return true;
            }
            if (gameManager.isGameRunning()) {
                player.sendMessage(ChatColor.RED + "Game already running!");
                return true;
            }
            if (gameManager.isInLobby(player)) {
                player.sendMessage(ChatColor.YELLOW + "You are already in the lobby!");
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
            Location tower = gameManager.getTowerLocations().get(new java.util.Random().nextInt(gameManager.getTowerLocations().size()));
            player.teleport(tower);
            player.sendMessage(ChatColor.GREEN + "Teleported to a Towers game tower!");
            return true;
        }
        player.sendMessage(ChatColor.RED + "Unknown subcommand! Use /towers <setup|tp>");
        return true;
    }
}