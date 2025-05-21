package dev.rokyytr.generators;

import dev.rokyytr.managers.GameManager;
import org.bukkit.*;
import org.bukkit.block.Biome;

import java.util.ArrayList;
import java.util.List;

public class WorldGenerator {
    private final GameManager gameManager;

    public WorldGenerator(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void createGameWorld() {
        String worldName = "towers_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"structures\": {}, \"layers\": []}");
        creator.generateStructures(false);
        World gameWorld = creator.createWorld();
        assert gameWorld != null;
        gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameManager.setGameWorld(gameWorld);
        generateTowers();
    }

    private void generateTowers() {
        List<Location> towerLocations = new ArrayList<>();
        World gameWorld = gameManager.getGameWorld();
        int numberOfTowers = gameManager.getNumberOfTowers();
        int bottomY = gameManager.getTowerBottomY();
        int height = gameManager.getTowerMinHeight();
        double radius = gameManager.getTowerRadius();
        Material towerMaterial = gameManager.getTowerMaterial();

        for (int i = 0; i < numberOfTowers; i++) {
            double angle = 2 * Math.PI * i / numberOfTowers;
            int x = (int) Math.round(radius * Math.cos(angle));
            int z = (int) Math.round(radius * Math.sin(angle));
            for (int y = bottomY; y < bottomY + height; y++) {
                gameWorld.getBlockAt(x, y, z).setType(towerMaterial);
            }
            towerLocations.add(new Location(gameWorld, x, bottomY + height, z));
        }
        gameManager.getTowerLocations().clear();
        gameManager.getTowerLocations().addAll(towerLocations);
    }

    public void setWorldBiome() {
        World gameWorld = gameManager.getGameWorld();
        Biome biome;
        switch (gameManager.getSelectedBiome()) {
            case "nether":
                biome = Biome.NETHER_WASTES;
                break;
            case "end":
                biome = Biome.THE_END;
                break;
            default:
                biome = Biome.PLAINS;
        }
        int radius = (int) gameManager.getTowerRadius() + 10;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                gameWorld.setBiome(x, 0, z, biome);
            }
        }
    }
}