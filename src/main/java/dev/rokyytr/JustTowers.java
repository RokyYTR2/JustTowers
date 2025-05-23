package dev.rokyytr;

import dev.rokyytr.commands.TowersCommand;
import dev.rokyytr.commands.TowersTabCompleter;
import dev.rokyytr.listeners.EventListener;
import dev.rokyytr.managers.GameManager;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;

public class JustTowers extends JavaPlugin {
    private static final String URL = "https://pastebin.com/raw/4UafwhLB";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        GameManager gameManager = new GameManager(this);
        getServer().getPluginManager().registerEvents(new EventListener(gameManager), this);
        Objects.requireNonNull(getCommand("towers")).setExecutor(new TowersCommand(gameManager));
        Objects.requireNonNull(getCommand("towers")).setTabCompleter(new TowersTabCompleter());
        checkVersion();
        System.out.println("JustTowers has been enabled!");
    }

    @Override
    public void onDisable() {
        System.out.println("JustTowers has been disabled!");
    }

    private void checkVersion() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String pastebinVersion = fetchPastebinVersion();
                    if (pastebinVersion == null) {
                        getLogger().warning("Failed to fetch version from Pastebin. Check the URL or network connection.");
                        return;
                    }

                    String localVersion = getDescription().getVersion();
                    ComparableVersion local = new ComparableVersion(localVersion);
                    ComparableVersion remote = new ComparableVersion(pastebinVersion);

                    if (remote.compareTo(local) > 0) {
                        getLogger().warning("You are running an outdated version of JustTowers! Local version: " + localVersion + ", Latest version: " + pastebinVersion);
                    } else {
                        getLogger().info("JustTowers is up to date. Version: " + localVersion);
                    }
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Error checking version: " + e.getMessage(), e);
                }
            }
        }.runTaskAsynchronously(this);
    }

    private String fetchPastebinVersion() throws IOException {
        URL url = new URL(URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            getLogger().warning("Pastebin returned HTTP " + responseCode);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String version = reader.readLine();
            if (version != null) {
                return version.trim();
            }
            return null;
        } finally {
            connection.disconnect();
        }
    }
}