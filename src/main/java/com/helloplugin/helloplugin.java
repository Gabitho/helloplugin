package com.pvpheads;

import com.tonplugin.commands.fakekillcommand;

import org.bukkit.plugin.java.JavaPlugin;
import com.pvphead.listeners.PlayerDeathListener;

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
