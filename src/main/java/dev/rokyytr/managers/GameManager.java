package dev.rokyytr.managers;

import dev.rokyytr.JustTowers;
import dev.rokyytr.generators.WorldGenerator;
import dev.rokyytr.gui.GuiManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private final JustTowers plugin;
    private final WorldGenerator worldGenerator;
    private final GuiManager guiManager;
    private World gameWorld;
    private final List<Location> towerLocations = new ArrayList<>();
    private final List<Location> availableTowerLocations = new ArrayList<>();
    private final List<Player> lobbyPlayers = new ArrayList<>();
    private boolean gameRunning = false;
    private int playerCountToStart;
    private final int numberOfTowers;
    private final int towerBottomY;
    private final int towerMinHeight;
    private final int towerMaxHeight;
    private final double towerRadius;
    private final double towerGap;
    private final Material towerMaterial;
    private String selectedMode;
    private String selectedBiome;
    private final int itemInterval;
    private final double risingSpeed;
    private final double waterDamage;
    private final boolean enableModeVoting;
    private final boolean enableBiomeVoting;
    private final String defaultMode;
    private final String defaultBiome;
    private final int borderShrinkInterval;
    private final double borderSize;
    private final Random random = ThreadLocalRandom.current();

    public GameManager(JustTowers plugin) {
        this.plugin = plugin;
        this.numberOfTowers = plugin.getConfig().getInt("towers.number", 8);
        this.towerBottomY = plugin.getConfig().getInt("towers.bottom_y", 64);
        this.towerMinHeight = plugin.getConfig().getInt("towers.min_height", 20);
        this.towerMaxHeight = plugin.getConfig().getInt("towers.max_height", 20);
        this.towerRadius = plugin.getConfig().getDouble("towers.radius", 16);
        this.towerGap = plugin.getConfig().getDouble("towers.gap", 5.0);
        String materialName = plugin.getConfig().getString("towers.material", "BEDROCK");
        this.towerMaterial = Material.getMaterial(materialName) != null ? Material.getMaterial(materialName) : Material.BEDROCK;
        this.itemInterval = plugin.getConfig().getInt("gameplay.item-interval", 10);
        this.risingSpeed = plugin.getConfig().getDouble("gameplay.rising-speed", 1.0);
        this.waterDamage = plugin.getConfig().getDouble("gameplay.water-damage", 2.0);
        this.enableModeVoting = plugin.getConfig().getBoolean("voting.enable-mode-voting", true);
        this.enableBiomeVoting = plugin.getConfig().getBoolean("voting.enable-biome-voting", true);
        this.defaultMode = plugin.getConfig().getString("voting.default-mode", "void");
        this.defaultBiome = plugin.getConfig().getString("voting.default-biome", "overworld");
        this.selectedMode = enableModeVoting ? "void" : defaultMode;
        this.selectedBiome = enableBiomeVoting ? "overworld" : defaultBiome;
        this.borderShrinkInterval = plugin.getConfig().getInt("gameplay.border-shrink-interval", 15);
        this.borderSize = plugin.getConfig().getDouble("gameplay.border-size", 64.0);
        this.playerCountToStart = plugin.getConfig().getInt("gameplay.player-count-to-start", 2);
        this.worldGenerator = new WorldGenerator(this);
        this.guiManager = new GuiManager(this);
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
        if (!availableTowerLocations.isEmpty()) {
            Location spawnLoc = availableTowerLocations.remove(0);
            player.teleport(spawnLoc.clone().add(0.5, 0, 0.5));
        } else {
            player.sendMessage(ChatColor.RED + "No available towers to spawn in!");
            return;
        }
        if (lobbyPlayers.size() >= playerCountToStart && !gameRunning) {
            startCountdown();
        }
    }

    public void addPlayer(Player player) {
        if (gameWorld != null && player.getWorld().equals(gameWorld) && !gameRunning) {
            lobbyPlayers.add(player);
            guiManager.openVotingGui(player);
            if (!availableTowerLocations.isEmpty()) {
                Location spawnLoc = availableTowerLocations.remove(0);
                player.teleport(spawnLoc.clone().add(0.5, 0, 0.5));
            } else {
                player.sendMessage(ChatColor.RED + "No available towers to spawn in!");
                return;
            }
            if (lobbyPlayers.size() >= playerCountToStart) {
                startCountdown();
            }
        }
    }

    public void updatePlayerCount(boolean increase) {
        playerCountToStart = Math.min(Math.max(playerCountToStart + (increase ? 1 : -1), 2), 16);
    }

    public void handleVote(Player player, String vote) {
        if (vote.contains("Rising Lava")) selectedMode = "lava";
        else if (vote.contains("Rising Water")) selectedMode = "water";
        else if (vote.contains("Void")) selectedMode = "void";
        else if (vote.contains("Overworld Biome")) selectedBiome = "overworld";
        else if (vote.contains("Nether Biome")) selectedBiome = "nether";
        else if (vote.contains("End Biome")) selectedBiome = "end";
        player.sendMessage(ChatColor.GREEN + "Voted for " + vote);
        if (lobbyPlayers.size() >= playerCountToStart && !gameRunning) {
            startCountdown();
        }
    }

    private void createLobbyWorld() {
        worldGenerator.createGameWorld();
        availableTowerLocations.addAll(towerLocations);
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
        for (Location loc : towerLocations) {
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            for (int dy = 0; dy < 2; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0 && dy == 0) continue;
                        gameWorld.getBlockAt(x + dx, y + dy, z + dz).setType(Material.AIR);
                    }
                }
            }
        }
        List<Location> locations = new ArrayList<>(towerLocations);
        for (int i = 0; i < lobbyPlayers.size(); i++) {
            Player player = lobbyPlayers.get(i);
            if (i < locations.size()) {
                Location loc = locations.get(i);
                player.teleport(new Location(gameWorld, loc.getX(), towerBottomY + towerMinHeight, loc.getZ()).add(0.5, 0, 0.5));
            } else {
                player.teleport(new Location(gameWorld, locations.get(0).getX(), towerBottomY + towerMinHeight, locations.get(0).getZ()).add(0.5, 0, 0.5));
            }
        }
        lobbyPlayers.clear();
        runGameTasks();
    }

    private void runGameTasks() {
        final double[] currentBorderSize = {borderSize};
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
                            } else if (selectedMode.equals("border")) {
                                double dist = Math.sqrt(x * x + z * z);
                                if (dist > currentBorderSize[0]) {
                                    gameWorld.getBlockAt(x, intYLevel, z).setType(Material.BARRIER);
                                }
                            }
                        }
                    }
                }
                if (selectedMode.equals("border")) {
                    currentBorderSize[0] -= 1.0;
                    if (currentBorderSize[0] < 5) currentBorderSize[0] = 5;
                }
                for (Player p : gameWorld.getPlayers()) {
                    if (p.getLocation().getY() <= yLevel && selectedMode.equals("water")) {
                        p.damage(waterDamage);
                    }
                    if (p.getLocation().getY() < yLevel - 2 || (selectedMode.equals("border") && Math.sqrt(p.getLocation().getX() * p.getLocation().getX() + p.getLocation().getZ() * p.getLocation().getZ()) > currentBorderSize[0])) {
                        if (p.getHealth() > 0) {
                            p.setGameMode(GameMode.SPECTATOR);
                            p.sendTitle(ChatColor.RED + "You Died!", "", 0, 40, 0);
                            p.setHealth(20.0);
                        }
                    }
                }
                long alive = gameWorld.getPlayers().stream().filter(p -> p.getHealth() > 0 && p.getGameMode() != GameMode.SPECTATOR).count();
                if (alive <= 1) {
                    gameRunning = false;
                    cancel();
                    for (Player p : gameWorld.getPlayers()) {
                        if (p.getHealth() > 0 && p.getGameMode() != GameMode.SPECTATOR) {
                            p.sendMessage(ChatColor.GOLD + "You won!");
                        }
                        p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                    }
                    Bukkit.getServer().unloadWorld(gameWorld, false);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }
                if (selectedMode.equals("border") && ticks % (borderShrinkInterval * 20) == 0) {
                    currentBorderSize[0] -= 1.0;
                    if (currentBorderSize[0] < 5) currentBorderSize[0] = 5;
                }
                ticks++;
                for (Player p : gameWorld.getPlayers()) {
                    if (p.getHealth() > 0 && p.getGameMode() != GameMode.SPECTATOR) {
                        p.getInventory().addItem(new ItemStack(getRandomMaterial()));
                    }
                }
            }
        }.runTaskTimer(plugin, itemInterval * 20L, 1L);
    }

    private Material getRandomMaterial() {
        Material[] materials = {
                Material.STONE, Material.WOODEN_SWORD, Material.BOW, Material.ARROW,
                Material.IRON_AXE, Material.GOLDEN_APPLE, Material.TNT, Material.FLINT_AND_STEEL,
                Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.OAK_PLANKS, Material.GLASS
        };
        return materials[random.nextInt(materials.length)];
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

    public String getSelectedMode() {
        return selectedMode;
    }

    public String getSelectedBiome() {
        return selectedBiome;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public int getNumberOfTowers() {
        return numberOfTowers;
    }

    public int getTowerBottomY() {
        return towerBottomY;
    }

    public int getTowerMinHeight() {
        return towerMinHeight;
    }

    public int getTowerMaxHeight() {
        return towerMaxHeight;
    }

    public double getTowerRadius() {
        return towerRadius;
    }

    public double getTowerGap() {
        return towerGap;
    }

    public Material getTowerMaterial() {
        return towerMaterial;
    }

    public boolean isModeVotingEnabled() {
        return enableModeVoting;
    }

    public boolean isBiomeVotingEnabled() {
        return enableBiomeVoting;
    }
}