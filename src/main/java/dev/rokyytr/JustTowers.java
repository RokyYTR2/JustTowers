package dev.rokyytr;

import dev.rokyytr.commands.TowersCommand;
import dev.rokyytr.commands.TowersTabCompleter;
import dev.rokyytr.listeners.EventListener;
import dev.rokyytr.managers.GameManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class JustTowers extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        GameManager gameManager = new GameManager(this);
        getServer().getPluginManager().registerEvents(new EventListener(gameManager), this);
        Objects.requireNonNull(getCommand("towers")).setExecutor(new TowersCommand(gameManager));
        Objects.requireNonNull(getCommand("towers")).setTabCompleter(new TowersTabCompleter());
    }
}