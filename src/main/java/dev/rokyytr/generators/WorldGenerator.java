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
        gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
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
        Material towerMaterial = gameManager.getTowerMaterial();

        for (int i = 0; i < numberOfTowers; i++) {
            double angle = 2 * Math.PI * i / numberOfTowers;
            int x = (int) Math.round((radius + gap * i) * Math.cos(angle));
            int z = (int) Math.round((radius + gap * i) * Math.sin(angle));
            int height = minHeight;

            for (int y = bottomY; y < bottomY + height; y++) {
                gameWorld.getBlockAt(x, y, z).setType(towerMaterial);
            }

            int boxBaseY = bottomY + height + 5;

            gameWorld.getBlockAt(x, boxBaseY, z).setType(Material.GLASS);

            gameWorld.getBlockAt(x + 1, boxBaseY, z).setType(Material.GLASS);
            gameWorld.getBlockAt(x - 1, boxBaseY, z).setType(Material.GLASS);
            gameWorld.getBlockAt(x, boxBaseY, z + 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x, boxBaseY, z - 1).setType(Material.GLASS);

            gameWorld.getBlockAt(x + 1, boxBaseY, z + 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x + 1, boxBaseY, z - 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x - 1, boxBaseY, z + 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x - 1, boxBaseY, z - 1).setType(Material.GLASS);

            for (int wallY = boxBaseY + 1; wallY <= boxBaseY + 2; wallY++) {
                gameWorld.getBlockAt(x + 1, wallY, z).setType(Material.GLASS);
                gameWorld.getBlockAt(x - 1, wallY, z).setType(Material.GLASS);
                gameWorld.getBlockAt(x, wallY, z + 1).setType(Material.GLASS);
                gameWorld.getBlockAt(x, wallY, z - 1).setType(Material.GLASS);

                gameWorld.getBlockAt(x + 1, wallY, z + 1).setType(Material.GLASS);
                gameWorld.getBlockAt(x + 1, wallY, z - 1).setType(Material.GLASS);
                gameWorld.getBlockAt(x - 1, wallY, z + 1).setType(Material.GLASS);
                gameWorld.getBlockAt(x - 1, wallY, z - 1).setType(Material.GLASS);
            }

            gameWorld.getBlockAt(x, boxBaseY + 3, z).setType(Material.GLASS);
            gameWorld.getBlockAt(x + 1, boxBaseY + 3, z).setType(Material.GLASS);
            gameWorld.getBlockAt(x - 1, boxBaseY + 3, z).setType(Material.GLASS);
            gameWorld.getBlockAt(x, boxBaseY + 3, z + 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x, boxBaseY + 3, z - 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x + 1, boxBaseY + 3, z + 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x + 1, boxBaseY + 3, z - 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x - 1, boxBaseY + 3, z + 1).setType(Material.GLASS);
            gameWorld.getBlockAt(x - 1, boxBaseY + 3, z - 1).setType(Material.GLASS);

            towerLocations.add(new Location(gameWorld, x, boxBaseY, z));
        }
        gameManager.getTowerLocations().clear();
        gameManager.getTowerLocations().addAll(towerLocations);
    }
}