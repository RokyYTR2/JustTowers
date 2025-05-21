package dev.rokyytr.managers;

import dev.rokyytr.JustTowers;
import dev.rokyytr.generators.WorldGenerator;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private final JustTowers plugin;
    private final WorldGenerator worldGenerator;
    private World gameWorld;
    private final List<Location> towerLocations = new ArrayList<>();
    private final List<Player> lobbyPlayers = new ArrayList<>();
    private boolean gameRunning = false;
    private final int playerCountToStart = 2;
    private int numberOfTowers = 8;
    private final int towerBottomY = 64;
    private final int towerMinHeight = 20;
    private int towerMaxHeight = 20;
    private double towerRadius = 16;
    private final Material towerMaterial = Material.BEDROCK;
    private String selectedBiome = "overworld";
    private int itemInterval = 10;
    private final double waterDamage = 2.0;
    private final double risingSpeed = 1.0;
    private final String selectedMode = "void";

    public GameManager(JustTowers plugin) {
        this.plugin = plugin;
        this.worldGenerator = new WorldGenerator(this);
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public boolean isInLobby(Player player) {
        return lobbyPlayers.contains(player);
    }

    public void addPlayerToLobby(Player player) {
        if (gameWorld == null) {
            createLobbyWorld();
        }
        lobbyPlayers.add(player);
        buildGlassBoxLobby();
        Location lobbyLocation = getLobbyLocation();
        player.teleport(lobbyLocation.clone().add(0.5, 1, 0.5));
        if (lobbyPlayers.size() >= playerCountToStart && !gameRunning) {
            startCountdown();
        }
    }

    private void createLobbyWorld() {
        worldGenerator.createGameWorld();
    }

    private Location getLobbyLocation() {
        return new Location(gameWorld, 0, towerBottomY + towerMinHeight + 10, 0);
    }

    private void buildGlassBoxLobby() {
        Location lobbyLocation = getLobbyLocation();
        World world = gameWorld;
        int x0 = lobbyLocation.getBlockX();
        int y0 = lobbyLocation.getBlockY();
        int z0 = lobbyLocation.getBlockZ();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean wall = x == -1 || x == 1 || y == 0 || y == 2 || z == -1 || z == 1;
                    world.getBlockAt(x0 + x, y0 + y, z0 + z).setType(wall ? Material.GLASS : Material.AIR);
                }
            }
        }
    }

    private void startCountdown() {
        gameRunning = true;
        new BukkitRunnable() {
            int countdown = 5;
            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player p : lobbyPlayers) {
                        p.sendTitle(ChatColor.GOLD + "" + countdown, "", 0, 20, 0);
                    }
                    countdown--;
                } else {
                    for (Player p : lobbyPlayers) {
                        p.sendTitle(ChatColor.RED + "Fight!", "", 0, 40, 0);
                    }
                    startGame();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    private void startGame() {
        worldGenerator.generateTowers();
        List<Location> locations = new ArrayList<>(towerLocations);
        for (int i = 0; i < lobbyPlayers.size(); i++) {
            Player player = lobbyPlayers.get(i);
            if (i < locations.size()) {
                player.teleport(locations.get(i).clone().add(0.5, 1, 0.5));
            } else {
                player.teleport(locations.get(0).clone().add(0.5, 1, 0.5));
            }
        }
        lobbyPlayers.clear();
        runGameTasks();
    }

    private void runGameTasks() {
        new BukkitRunnable() {
            double yLevel = 0;
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }
                if (yLevel < 104) {
                    yLevel += risingSpeed;
                    int intYLevel = (int) yLevel;
                    for (int x = -64; x <= 64; x++) {
                        for (int z = -64; z <= 64; z++) {
                            if (selectedMode.equals("lava")) {
                                gameWorld.getBlockAt(x, intYLevel, z).setType(Material.LAVA);
                            } else if (selectedMode.equals("water")) {
                                gameWorld.getBlockAt(x, intYLevel, z).setType(Material.WATER);
                            }
                        }
                    }
                }
                for (Player p : gameWorld.getPlayers()) {
                    if (p.getLocation().getY() <= yLevel && selectedMode.equals("water")) {
                        p.damage(waterDamage);
                    }
                    if (p.getLocation().getY() < yLevel - 2) {
                        p.setHealth(0);
                    }
                }
                long alive = gameWorld.getPlayers().stream().filter(p -> p.getHealth() > 0).count();
                if (alive <= 1) {
                    gameRunning = false;
                    cancel();
                    for (Player p : gameWorld.getPlayers()) {
                        if (p.getHealth() > 0) {
                            p.sendMessage(ChatColor.GOLD + "You won!");
                        }
                        p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                    }
                    Bukkit.getServer().unloadWorld(gameWorld, false);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public World getGameWorld() {
        return gameWorld;
    }

    public void setGameWorld(World world) {
        this.gameWorld = world;
    }

    public List<Location> getTowerLocations() {
        return towerLocations;
    }

    public int getPlayerCountToStart() {
        return playerCountToStart;
    }

    public String getSelectedBiome() {
        return selectedBiome;
    }

    public int getNumberOfTowers() {
        return numberOfTowers;
    }

    public int getTowerBottomY() { return towerBottomY; }

    public int getTowerMinHeight() { return towerMinHeight; }

    public int getTowerMaxHeight() { return towerMaxHeight; }

    public double getTowerRadius() { return towerRadius; }

    public Material getTowerMaterial() {
        return towerMaterial;
    }
}