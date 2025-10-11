package com.pvpheads;

import com.pvpheads.commands.FakeKillCommand;
import com.pvpheads.listeners.PlayerDeathListener;
import com.pvpheads.listeners.DragonSwordListener;
import com.pvpheads.recipes.DragonSwordRecipe;


import com.pvpheads.displaymob.DisplayMobManager;
import com.pvpheads.displaymob.DisplayMobListener;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Plugin PvpHeads activé !");
        
        // Enregistre les listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(new DragonSwordListener(), this);
        

        // Enregistre la commande /fakekill
        this.getCommand("fakekill").setExecutor(new FakeKillCommand());

        // Enregistre la recette de l'épée du dragon
        new DragonSwordRecipe(this);

        //Mob
        DisplayMobManager displayMobManager = new DisplayMobManager(this);
        DisplayMobListener displayMobListener = new DisplayMobListener(this, displayMobManager);
        getServer().getPluginManager().registerEvents(displayMobListener, this);

        // spawn test (delai pour que le monde soit prêt)
        getServer().getScheduler().runTaskLater(this, () -> {
            displayMobManager.spawn(getServer().getWorlds().get(0).getSpawnLocation().clone().add(2,1,2), 1.1f, 3);
        }, 40L);

    }

    @Override
    public void onDisable(){
        displayMobManager.removeAll();
    }
}