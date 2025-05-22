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
            player.sendMessage(ChatColor.RED + "Usage: /towers <setup|tp|join|reload>");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "setup":
                return handleSetup(player);
            case "tp":
                return handleTeleport(player);
            case "join":
                return handleJoin(player);
            case "reload":
                return handleReload(player);
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand! Use /towers <setup|tp|join|reload>");
                return true;
        }
    }

    private boolean handleSetup(Player player) {
        if (!player.hasPermission("towers.setup")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        if (gameManager.isGameRunning()) {
            player.sendMessage(ChatColor.RED + "Cannot setup while a game is running!");
            return true;
        }

        if (gameManager.getGameWorld() == null) {
            player.sendMessage(ChatColor.YELLOW + "Creating towers world...");
            gameManager.createLobbyWorld();
            player.sendMessage(ChatColor.GREEN + "Towers world created and towers generated!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Towers world already exists. Use /towers tp or /towers join to enter.");
        }

        gameManager.getGuiManager().openSetupGui(player);
        return true;
    }

    private boolean handleTeleport(Player player) {
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

    private boolean handleJoin(Player player) {
        if (gameManager.getGameWorld() == null) {
            player.sendMessage(ChatColor.RED + "No Towers world is active! Ask an admin to use /towers setup first.");
            return true;
        }

        gameManager.addPlayerToLobby(player);
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("towers.reload")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        if (gameManager.isGameRunning()) {
            player.sendMessage(ChatColor.RED + "Cannot reload config while a game is running!");
            return true;
        }

        try {
            gameManager.loadConfig();
            player.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}