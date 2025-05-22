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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private final JustTowers plugin;
    private final WorldGenerator worldGenerator;
    private final GuiManager guiManager;
    private World gameWorld;
    private final List<Location> towerLocations = new ArrayList<>();
    private final List<Location> availableTowerLocations = new ArrayList<>();
    private final List<Player> lobbyPlayers = new ArrayList<>();
    private final Map<String, Integer> modeVotes = new HashMap<>();
    private final Map<String, Integer> biomeVotes = new HashMap<>();
    private final Set<Player> votedPlayers = new HashSet<>();
    private final BossBar itemBossBar;
    private WorldBorder worldBorder;
    private boolean gameRunning = false;
    private int playerCountToStart;
    private int countdownTime;
    private int numberOfTowers;
    private int towerBottomY;
    private int towerMinHeight;
    private int towerMaxHeight;
    private double towerRadius;
    private double towerGap;
    private List<Material> towerMaterials;
    private String selectedMode;
    private String selectedBiome;
    private int itemInterval;
    private double risingSpeed;
    private double risingDamage;
    private boolean enableModeVoting;
    private boolean enableBiomeVoting;
    private String defaultMode;
    private String defaultBiome;
    private int borderShrinkInterval;
    private double initialBorderSize;
    private double minBorderSize;
    private int voteDuration;
    private final Random random = ThreadLocalRandom.current();

    public GameManager(JustTowers plugin) {
        this.plugin = plugin;
        loadConfig();
        this.worldGenerator = new WorldGenerator(this);
        this.guiManager = new GuiManager(this);
        this.itemBossBar = Bukkit.createBossBar("Next item in: " + itemInterval + "s", BarColor.GREEN, BarStyle.SOLID);
        initializeVotes();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.numberOfTowers = plugin.getConfig().getInt("towers.number", 8);
        this.towerBottomY = plugin.getConfig().getInt("towers.bottom_y", 64);
        this.towerMinHeight = plugin.getConfig().getInt("towers.height.min", 20);
        this.towerMaxHeight = plugin.getConfig().getInt("towers.height.max", 20);
        this.towerRadius = plugin.getConfig().getDouble("towers.radius", 16.0);
        this.towerGap = plugin.getConfig().getDouble("towers.gap", 5.0);

        List<String> materialNames = plugin.getConfig().getStringList("towers.materials");
        if (materialNames.isEmpty()) {
            materialNames.add("BEDROCK");
        }
        this.towerMaterials = new ArrayList<>();
        for (String materialName : materialNames) {
            Material mat = Material.getMaterial(materialName);
            if (mat != null) {
                this.towerMaterials.add(mat);
            }
        }
        if (this.towerMaterials.isEmpty()) {
            this.towerMaterials.add(Material.BEDROCK);
        }

        this.itemInterval = plugin.getConfig().getInt("gameplay.item.interval", 10);
        this.risingSpeed = plugin.getConfig().getDouble("gameplay.rising.speed", 1.0);
        this.risingDamage = plugin.getConfig().getDouble("gameplay.rising.damage", 2.0);
        this.enableModeVoting = plugin.getConfig().getBoolean("voting.enabled.mode", true);
        this.enableBiomeVoting = plugin.getConfig().getBoolean("voting.enabled.biome", true);
        this.defaultMode = plugin.getConfig().getString("voting.defaults.mode", "void");
        this.defaultBiome = plugin.getConfig().getString("voting.defaults.biome", "overworld");
        this.selectedMode = enableModeVoting ? null : defaultMode;
        this.selectedBiome = enableBiomeVoting ? null : defaultBiome;
        this.borderShrinkInterval = plugin.getConfig().getInt("gameplay.border.shrink-interval", 15);
        this.initialBorderSize = plugin.getConfig().getDouble("gameplay.border.initial-size", 64.0);
        this.minBorderSize = plugin.getConfig().getDouble("gameplay.border.min-size", 8.0);
        this.playerCountToStart = plugin.getConfig().getInt("gameplay.start.player-count", 2);
        this.countdownTime = plugin.getConfig().getInt("gameplay.start.countdown", 30);
        this.voteDuration = plugin.getConfig().getInt("voting.vote-duration", 30);
    }

    private void initializeVotes() {
        modeVotes.clear();
        biomeVotes.clear();
        modeVotes.put("lava", 0);
        modeVotes.put("water", 0);
        modeVotes.put("void", 0);
        modeVotes.put("border", 0);
        biomeVotes.put("overworld", 0);
        biomeVotes.put("nether", 0);
        biomeVotes.put("end", 0);
    }

    public JustTowers getPlugin() {
        return plugin;
    }

    public void addPlayerToLobby(Player player) {
        if (gameRunning) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&cGame is already running!"));
            return;
        }

        if (lobbyPlayers.contains(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&cYou are already in the game!"));
            return;
        }

        if (gameWorld == null) {
            createLobbyWorld();
        }

        if (availableTowerLocations.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&cNo available towers to spawn in!"));
            return;
        }

        lobbyPlayers.add(player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);

        Location spawnLoc = availableTowerLocations.remove(0);
        player.teleport(spawnLoc.clone().add(0.5, 1, 0.5));

        ItemStack votingItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = votingItem.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.GOLD + "Vote for Game Settings");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Right-click to open voting menu"));
        votingItem.setItemMeta(meta);
        player.getInventory().setItem(4, votingItem);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aYou joined the game! (" + lobbyPlayers.size() + "/" + playerCountToStart + ")"));

        if (lobbyPlayers.size() >= playerCountToStart) {
            startCountdown();
        }
    }

    public void addPlayer(Player player) {
        if (gameWorld != null && player.getWorld().equals(gameWorld) && !gameRunning) {
            addPlayerToLobby(player);
        }
    }

    public void updatePlayerCount(boolean increase) {
        playerCountToStart = Math.min(Math.max(playerCountToStart + (increase ? 1 : -1), 2), Math.min(16, numberOfTowers));
    }

    public void handleVote(Player player, String vote) {
        if (votedPlayers.contains(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&cYou have already voted!"));
            return;
        }

        if (vote.contains("Rising Lava")) {
            modeVotes.put("lava", modeVotes.get("lava") + 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aVoted for Rising Lava"));
        } else if (vote.contains("Rising Water")) {
            modeVotes.put("water", modeVotes.get("water") + 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aVoted for Rising Water"));
        } else if (vote.contains("Void")) {
            modeVotes.put("void", modeVotes.get("void") + 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aVoted for Void"));
        } else if (vote.contains("Shrinking Border")) {
            modeVotes.put("border", modeVotes.get("border") + 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aVoted for Shrinking Border"));
        } else if (vote.contains("Overworld Biome")) {
            biomeVotes.put("overworld", biomeVotes.get("overworld") + 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aVoted for Overworld Biome"));
        } else if (vote.contains("Nether Biome")) {
            biomeVotes.put("nether", biomeVotes.get("nether") + 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aVoted for Nether Biome"));
        } else if (vote.contains("End Biome")) {
            biomeVotes.put("end", biomeVotes.get("end") + 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&aVoted for End Biome"));
        }
    }

    private void determineWinners() {
        if (enableModeVoting) {
            selectedMode = modeVotes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(defaultMode);
        } else {
            selectedMode = defaultMode;
        }

        if (enableBiomeVoting) {
            selectedBiome = biomeVotes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(defaultBiome);
        } else {
            selectedBiome = defaultBiome;
        }
    }

    public void createLobbyWorld() {
        worldGenerator.createGameWorld();
        availableTowerLocations.addAll(towerLocations);

        if (gameWorld != null) {
            worldBorder = gameWorld.getWorldBorder();
            worldBorder.setCenter(0, 0);
            worldBorder.setSize(initialBorderSize * 2);
        }
    }

    private void startCountdown() {
        if (gameRunning) return;

        determineWinners();

        String modeText = selectedMode.substring(0, 1).toUpperCase() + selectedMode.substring(1);
        String biomeText = selectedBiome.substring(0, 1).toUpperCase() + selectedBiome.substring(1);

        for (Player p : lobbyPlayers) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&eWinning mode: &b" + modeText));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&eWinning biome: &b" + biomeText));
            p.closeInventory();
        }

        new BukkitRunnable() {
            int countdown = countdownTime;
            @Override
            public void run() {
                if (countdown > 0) {
                    for (Player p : lobbyPlayers) {
                        if (countdown <= 10 || countdown % 10 == 0) {
                            p.sendTitle(ChatColor.GOLD + "" + countdown, ChatColor.YELLOW + "Get ready!", 0, 20, 0);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        }
                    }
                    countdown--;
                } else {
                    for (Player p : lobbyPlayers) {
                        p.sendTitle(ChatColor.RED + "Fight!", ChatColor.DARK_RED + "Last player standing wins!", 0, 40, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }
                    startGame();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    private void startGame() {
        gameRunning = true;

        for (Location loc : towerLocations) {
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            for (int dy = 0; dy <= 4; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        gameWorld.getBlockAt(x + dx, y + dy, z + dz).setType(Material.AIR);
                    }
                }
            }
        }

        for (Player p : new ArrayList<>(lobbyPlayers)) {
            if (p.isOnline() && p.getWorld().equals(gameWorld)) {
                itemBossBar.addPlayer(p);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
                p.getInventory().clear();
            } else {
                lobbyPlayers.remove(p);
            }
        }

        runGameTasks();
    }

    private void runGameTasks() {
        new BukkitRunnable() {
            double yLevel = towerBottomY;
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }

                List<Player> alivePlayers = new ArrayList<>();
                for (Player p : gameWorld.getPlayers()) {
                    if (p.getGameMode() == GameMode.SURVIVAL && p.getHealth() > 0) {
                        alivePlayers.add(p);
                    }
                }

                if (alivePlayers.size() <= 1) {
                    endGame(alivePlayers.isEmpty() ? null : alivePlayers.get(0));
                    return;
                }

                if (!selectedMode.equals("border") && yLevel < towerBottomY + towerMinHeight + 20) {
                    yLevel += risingSpeed / 20.0;
                    int intYLevel = (int) yLevel;

                    int range = (int) (initialBorderSize + 10);
                    for (int x = -range; x <= range; x++) {
                        for (int z = -range; z <= range; z++) {
                            switch (selectedMode) {
                                case "lava":
                                    if (gameWorld.getBlockAt(x, intYLevel, z).getType() == Material.AIR) {
                                        gameWorld.getBlockAt(x, intYLevel, z).setType(Material.LAVA);
                                    }
                                    break;
                                case "water":
                                    if (gameWorld.getBlockAt(x, intYLevel, z).getType() == Material.AIR) {
                                        gameWorld.getBlockAt(x, intYLevel, z).setType(Material.WATER);
                                    }
                                    break;
                            }
                        }
                    }
                }

                for (Player p : alivePlayers) {
                    Location loc = p.getLocation();

                    if (selectedMode.equals("water") && loc.getY() <= yLevel) {
                        p.damage(risingDamage);
                    }

                    if ((selectedMode.equals("void") && loc.getY() < yLevel - 2) ||
                            (selectedMode.equals("lava") && loc.getY() <= yLevel)) {
                        eliminatePlayer(p);
                    }

                    if (selectedMode.equals("border")) {
                        double distance = Math.sqrt(loc.getX() * loc.getX() + loc.getZ() * loc.getZ());
                        if (distance > worldBorder.getSize() / 2) {
                            p.damage(1.0);
                            if (distance > worldBorder.getSize() / 2 + 5) {
                                eliminatePlayer(p);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L);

        new BukkitRunnable() {
            int timeLeft = itemInterval;
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    Material[] allMaterials = Material.values();

                    for (Player p : gameWorld.getPlayers()) {
                        if (p.getHealth() > 0 && p.getGameMode() == GameMode.SURVIVAL) {
                            Material randomMaterial;
                            do {
                                randomMaterial = allMaterials[random.nextInt(allMaterials.length)];
                            } while (!randomMaterial.isItem() || randomMaterial.isAir() || randomMaterial.name().contains("SPAWN"));

                            p.getInventory().addItem(new ItemStack(randomMaterial));
                        }
                    }
                    timeLeft = itemInterval;
                }

                itemBossBar.setTitle("Next item in: " + timeLeft + "s");
                itemBossBar.setProgress((double) timeLeft / itemInterval);
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        if (selectedMode.equals("border")) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!gameRunning) {
                        cancel();
                        return;
                    }

                    double newSize = Math.max(worldBorder.getSize() - 4, minBorderSize);
                    worldBorder.setSize(newSize, borderShrinkInterval);

                    for (Player p : gameWorld.getPlayers()) {
                        if (p.getGameMode() == GameMode.SURVIVAL) {
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&cBorder is shrinking! New size: " + (int)(newSize/2) + " blocks"));
                        }
                    }
                }
            }.runTaskTimer(plugin, borderShrinkInterval * 20L, borderShrinkInterval * 20L);
        }
    }

    private void eliminatePlayer(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.sendTitle(ChatColor.RED + "You Died!", ChatColor.GRAY + "Better luck next time!", 0, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);

        for (Player p : gameWorld.getPlayers()) {
            if (p != player) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&c" + player.getName() + " &cwas eliminated!"));
            }
        }
    }

    private void endGame(Player winner) {
        gameRunning = false;
        itemBossBar.removeAll();

        if (winner != null) {
            winner.sendTitle(ChatColor.GOLD + "You Won!", ChatColor.YELLOW + "Congratulations!", 0, 60, 20);
            winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            winner.getInventory().clear();

            for (Player p : gameWorld.getPlayers()) {
                if (p != winner) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("general.prefix", "&b[Towers] ") + "&e" + winner.getName() + " won the game!"));
                }
            }
        } else {
            for (Player p : gameWorld.getPlayers()) {
                p.sendTitle(ChatColor.GRAY + "Game Over", ChatColor.DARK_GRAY + "No winners!", 0, 60, 20);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : new ArrayList<>(gameWorld.getPlayers())) {
                    p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setHealth(20.0);
                    p.setFoodLevel(20);
                    p.getInventory().clear();
                    for (PotionEffect effect : p.getActivePotionEffects()) {
                        p.removePotionEffect(effect.getType());
                    }
                }

                Bukkit.getServer().unloadWorld(gameWorld, false);
                gameWorld = null;
                lobbyPlayers.clear();
                availableTowerLocations.clear();
                towerLocations.clear();
                votedPlayers.clear();
                initializeVotes();
            }
        }.runTaskLater(plugin, 100L);
    }

    public World getGameWorld() { return gameWorld; }

    public void setGameWorld(World world) { this.gameWorld = world; }

    public List<Location> getTowerLocations() { return towerLocations; }

    public int getPlayerCountToStart() { return playerCountToStart; }

    public GuiManager getGuiManager() { return guiManager; }

    public int getNumberOfTowers() { return numberOfTowers; }

    public int getTowerBottomY() { return towerBottomY; }

    public int getTowerMinHeight() { return towerMinHeight; }

    public int getTowerMaxHeight() { return towerMaxHeight; }

    public double getTowerRadius() { return towerRadius; }

    public double getTowerGap() { return towerGap; }

    public boolean isModeVotingEnabled() { return enableModeVoting; }

    public boolean isBiomeVotingEnabled() { return enableBiomeVoting; }

    public boolean isGameRunning() { return gameRunning; }

    public List<Material> getTowerMaterial() {
        return towerMaterials;
    }

    public List<Material> getTowerMaterials() {
        return towerMaterials;
    }
}