package dev.rokyytr.generators;

import dev.rokyytr.managers.GameManager;
import org.bukkit.*;

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

        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        gameWorld.setTime(6000);

        gameManager.setGameWorld(gameWorld);
        generateTowers();
    }

    public void generateTowers() {
        List<Location> towerLocations = new ArrayList<>();
        World gameWorld = gameManager.getGameWorld();
        int numberOfTowers = gameManager.getNumberOfTowers();
        int bottomY = gameManager.getTowerBottomY();
        int minHeight = gameManager.getTowerMinHeight();
        int maxHeight = gameManager.getTowerMaxHeight();
        double radius = gameManager.getTowerRadius();
        double gap = gameManager.getTowerGap();
        List<Material> towerMaterials = gameManager.getTowerMaterials();

        double angleStep = 2 * Math.PI / numberOfTowers;

        for (int i = 0; i < numberOfTowers; i++) {
            double angle = angleStep * i;
            double currentRadius = radius + (gap * i * 0.5);

            int x = (int) Math.round(currentRadius * Math.cos(angle));
            int z = (int) Math.round(currentRadius * Math.sin(angle));

            int height = minHeight;
            if (maxHeight > minHeight) {
                height = minHeight + (int)(Math.random() * (maxHeight - minHeight + 1));
            }

            Material towerMaterial = towerMaterials.get((int)(Math.random() * towerMaterials.size()));

            for (int y = bottomY; y < bottomY + height; y++) {
                gameWorld.getBlockAt(x, y, z).setType(towerMaterial);
            }

            int boxBaseY = bottomY + height;
            createSpawnBox(gameWorld, x, boxBaseY, z);

            towerLocations.add(new Location(gameWorld, x, boxBaseY + 1, z));
        }

        gameManager.getTowerLocations().clear();
        gameManager.getTowerLocations().addAll(towerLocations);
    }

    private void createSpawnBox(World world, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y, z + dz).setType(Material.GLASS);
            }
        }

        for (int wallY = y + 1; wallY <= y + 4; wallY++) {
            for (int dx = -1; dx <= 1; dx++) {
                world.getBlockAt(x + dx, wallY, z - 1).setType(Material.GLASS);
                world.getBlockAt(x + dx, wallY, z + 1).setType(Material.GLASS);
            }
            world.getBlockAt(x - 1, wallY, z).setType(Material.GLASS);
            world.getBlockAt(x + 1, wallY, z).setType(Material.GLASS);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    world.getBlockAt(x + dx, y + 5, z + dz).setType(Material.GLASS);
                }
            }
        }
    }
}