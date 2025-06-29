package com.pvpheads;

import com.pvpheads.commands.FakeKillCommand;
import org.bukkit.plugin.java.JavaPlugin;
import com.pvphead.listeners.PlayerDeathListener;
import com.pvphead.recipes.DragonSwordRecipe;

public class PvpHeadsPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Plugin PvpHeads activé !");
        
        // Enregistre les listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);

        // Enregistre la commande /fakekill
        this.getCommand("fakekill").setExecutor(new FakeKillCommand());

        // Enregistre la recette de l'épée du dragon
        DragonSwordRecipe.register(this);
    }
}