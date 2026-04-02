package com.pvpheads;

import com.pvpheads.commands.FakeKillCommand;
import com.pvpheads.listeners.PlayerDeathListener;
import com.pvpheads.listeners.DragonSwordListener;
import com.pvpheads.recipes.DragonSwordRecipe;


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
        this.getCommand("spawnPassiveMob").setExecutor(new SpawnPassiveMobCommand(this));

        // Enregistre la recette de l'épée du dragon
        new DragonSwordRecipe(this);

    }
}