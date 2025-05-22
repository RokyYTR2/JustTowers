package dev.rokyytr.managers;

import dev.rokyytr.JustTowers;
import dev.rokyytr.generators.WorldGenerator;
import dev.rokyytr.gui.GuiManager;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
    private final BossBar itemBossBar;
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
        this.itemBossBar = Bukkit.createBossBar("Next item in: 10s", BarColor.GREEN, BarStyle.SOLID);
    }

    public JustTowers getPlugin() {
        return plugin;
    }

    public void addPlayerToLobby(Player player) {
        if (lobbyPlayers.contains(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&b[Towers] ") + "&cYou are already in the game!"));
            return;
        }
        if (gameWorld == null) {
            createLobbyWorld();
        }
        lobbyPlayers.add(player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        if (!availableTowerLocations.isEmpty()) {
            Location spawnLoc = availableTowerLocations.remove(0);
            player.teleport(spawnLoc.clone().add(0.5, 1, 0.5));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&b[Towers] ") + "&cNo available towers to spawn in!"));
            return;
        }
        guiManager.openVotingGui(player);

        if (lobbyPlayers.size() >= playerCountToStart && !gameRunning) {
            startCountdown();
        }
    }

    public void addPlayer(Player player) {
        if (gameWorld != null && player.getWorld().equals(gameWorld) && !gameRunning) {
            if (lobbyPlayers.contains(player)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&b[Towers] ") + "&cYou are already in the game!"));
                return;
            }
            lobbyPlayers.add(player);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            guiManager.openVotingGui(player);
            if (!availableTowerLocations.isEmpty()) {
                Location spawnLoc = availableTowerLocations.remove(0);
                player.teleport(spawnLoc.clone().add(0.5, 1, 0.5));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&b[Towers] ") + "&cNo available towers to spawn in!"));
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
        else if (vote.contains("Shrinking Border")) selectedMode = "border";
        else if (vote.contains("Overworld Biome")) selectedBiome = "overworld";
        else if (vote.contains("Nether Biome")) selectedBiome = "nether";
        else if (vote.contains("End Biome")) selectedBiome = "end";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&b[Towers] ") + "&aVoted for " + vote));
        if (lobbyPlayers.size() >= playerCountToStart && !gameRunning) {
            startCountdown();
        }
    }

    public void createLobbyWorld() {
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

            for (int dy = 0; dy <= 3; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        gameWorld.getBlockAt(x + dx, y + dy, z + dz).setType(Material.AIR);
                    }
                }
            }
        }

        for (Player p : gameWorld.getPlayers()) {
            itemBossBar.addPlayer(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 30, 0));
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
                    yLevel += risingSpeed * 0.2;
                    int intYLevel = (int) yLevel;
                    for (int x = -32; x <= 32; x++) {
                        for (int z = -32; z <= 32; z++) {
                            switch (selectedMode) {
                                case "lava":
                                    gameWorld.getBlockAt(x, intYLevel, z).setType(Material.LAVA);
                                    break;
                                case "water":
                                    gameWorld.getBlockAt(x, intYLevel, z).setType(Material.WATER);
                                    break;
                                case "border":
                                    double dist = Math.sqrt(x * x + z * z);
                                    if (dist > currentBorderSize[0]) {
                                        gameWorld.getBlockAt(x, intYLevel, z).setType(Material.BARRIER);
                                    }
                                    break;
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
                    itemBossBar.removeAll();
                    for (Player p : gameWorld.getPlayers()) {
                        if (p.getHealth() > 0 && p.getGameMode() != GameMode.SPECTATOR) {
                            p.sendTitle(ChatColor.GOLD + "You Won!", "", 0, 40, 0);
                        }
                        p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                    }
                    Bukkit.getServer().unloadWorld(gameWorld, false);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        new BukkitRunnable() {
            int timeLeft = itemInterval;
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    for (Player p : gameWorld.getPlayers()) {
                        if (p.getHealth() > 0 && p.getGameMode() != GameMode.SPECTATOR) {
                            p.getInventory().addItem(new ItemStack(getRandomMaterial()));
                        }
                    }
                    timeLeft = itemInterval;
                }

                itemBossBar.setTitle("Next item in: " + timeLeft + "s");
                itemBossBar.setProgress((double) timeLeft / itemInterval);
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }
                if (selectedMode.equals("border")) {
                    currentBorderSize[0] -= 1.0;
                    if (currentBorderSize[0] < 5) currentBorderSize[0] = 5;
                }
            }
        }.runTaskTimer(plugin, borderShrinkInterval * 20L, borderShrinkInterval * 20L);
    }

    private Material getRandomMaterial() {
        Material[] allMaterials = Material.values();
        return allMaterials[random.nextInt(allMaterials.length)];
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

    public boolean isGameRunning() {
        return gameRunning;
    }
}