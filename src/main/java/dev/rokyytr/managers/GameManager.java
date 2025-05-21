package dev.rokyytr.managers;

import dev.rokyytr.JustTowers;
import dev.rokyytr.generators.WorldGenerator;
import dev.rokyytr.gui.GuiManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private final JustTowers plugin;
    private World gameWorld;
    private final List<Player> players = new ArrayList<>();
    private String selectedMode;
    private String selectedBiome;
    private int playerCountToStart = 2;
    private boolean gameRunning = false;
    private final List<Location> towerLocations = new ArrayList<>();
    private final Random random = ThreadLocalRandom.current();
    private final WorldGenerator worldGenerator;
    private final GuiManager guiManager;
    private final int numberOfTowers;
    private final int towerSpacing;
    private final int towerHeight;
    private final Material towerMaterial;
    private final int itemInterval;
    private final double risingSpeed;
    private final double waterDamage;
    private final boolean enableModeVoting;
    private final boolean enableBiomeVoting;
    private final String defaultMode;
    private final String defaultBiome;
    private final int towerBottomY;
    private final int towerMinHeight;
    private final int towerMaxHeight;
    private final double towerRadius;
    private final List<Player> lobbyPlayers = new ArrayList<>();
    private final Location lobbyLocation = new Location(Bukkit.getWorlds().get(0), 0, 100, 0);

    public GameManager(JustTowers plugin) {
        this.plugin = plugin;
        this.numberOfTowers = plugin.getConfig().getInt("towers.number", 16);
        this.towerSpacing = plugin.getConfig().getInt("towers.spacing", 16);
        this.towerHeight = plugin.getConfig().getInt("towers.height", 20);
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
        this.worldGenerator = new WorldGenerator(this);
        this.guiManager = new GuiManager(this);
        this.towerBottomY = plugin.getConfig().getInt("towers.bottom_y", 100);
        this.towerMinHeight = plugin.getConfig().getInt("towers.min_height", 15);
        this.towerMaxHeight = plugin.getConfig().getInt("towers.max_height", 25);
        this.towerRadius = plugin.getConfig().getDouble("towers.radius", 30.0);
    }

    public void setupGame(Player player) {
        if (!player.hasPermission("towers.setup")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return;
        }
        worldGenerator.createGameWorld();
        guiManager.openSetupGui(player);
    }

    public void addPlayer(Player player) {
        if (gameWorld != null && player.getWorld().equals(gameWorld) && !gameRunning) {
            players.add(player);
            guiManager.openVotingGui(player);
            player.teleport(towerLocations.get(players.size() - 1));
            checkStartCondition();
        }
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public boolean isInLobby(Player player) {
        return lobbyPlayers.contains(player);
    }

    public void addPlayerToLobby(Player player) {
        lobbyPlayers.add(player);
        buildGlassBoxLobby();
        Location tpLoc = lobbyLocation.clone().add(0, 1, 0);
        player.teleport(tpLoc);
        player.sendMessage(ChatColor.GREEN + "You joined the Towers lobby!");

        if (lobbyPlayers.size() >= playerCountToStart) {
            startCountdown();
        } else {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Players in lobby: " + lobbyPlayers.size() + "/" + playerCountToStart);
        }
    }

    private void buildGlassBoxLobby() {
        World world = lobbyLocation.getWorld();
        int x0 = lobbyLocation.getBlockX(), y0 = lobbyLocation.getBlockY(), z0 = lobbyLocation.getBlockZ();
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean wall = x == -1 || x == 1 || y == 0 || y == 2 || z == -1 || z == 1;
                    assert world != null;
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

    public void handleVote(Player player, String vote) {
        if (vote.contains("Rising Lava")) selectedMode = "lava";
        else if (vote.contains("Rising Water")) selectedMode = "water";
        else if (vote.contains("Void")) selectedMode = "void";
        else if (vote.contains("Overworld Biome")) selectedBiome = "overworld";
        else if (vote.contains("Nether Biome")) selectedBiome = "nether";
        else if (vote.contains("End Biome")) selectedBiome = "end";
        player.sendMessage(ChatColor.GREEN + "Voted for " + vote);
        checkStartCondition();
    }

    public void updatePlayerCount(boolean increase) {
        playerCountToStart = Math.min(Math.max(playerCountToStart + (increase ? 1 : -1), 2), 16);
    }

    private void checkStartCondition() {
        if (players.size() >= playerCountToStart) {
            startGame();
        }
    }

    private void startGame() {
        if (gameRunning) return;
        gameRunning = true;
        worldGenerator.setWorldBiome();
        setupGame((Player) lobbyPlayers);
        lobbyPlayers.clear();
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
                for (Player p : players) {
                    if (p.getLocation().getY() <= yLevel && selectedMode.equals("water")) {
                        p.damage(waterDamage);
                    }
                    if (p.getLocation().getY() < yLevel - 2) {
                        p.setHealth(0);
                    }
                }
                if (players.stream().filter(p -> p.getHealth() > 0).count() <= 1) {
                    gameRunning = false;
                    cancel();
                    for (Player p : players) {
                        if (p.getHealth() > 0) {
                            p.sendMessage(ChatColor.GOLD + "You won!");
                        }
                        p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                    }
                    Bukkit.getServer().unloadWorld(gameWorld, false);
                    players.clear();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }
                for (Player p : players) {
                    if (p.getHealth() > 0) {
                        p.getInventory().addItem(new ItemStack(getRandomMaterial()));
                    }
                }
            }
        }.runTaskTimer(plugin, itemInterval * 20L, itemInterval * 20L);
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

    public String getSelectedBiome() {
        return selectedBiome;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public int getNumberOfTowers() {
        return numberOfTowers;
    }

    public int getTowerSpacing() {
        return towerSpacing;
    }

    public int getTowerBottomY() { return towerBottomY; }

    public int getTowerMinHeight() { return towerMinHeight; }

    public int getTowerMaxHeight() { return towerMaxHeight; }

    public double getTowerRadius() { return towerRadius; }

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