package com.pvpheads.commands;

import com.pvpheads.Main;
import com.pvpheads.control.MobControl;
import com.pvpheads.control.MobControlFactory;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

//placeholder pour tester la commande de spawn de mob passif.

public class SpawnPassiveMobCommand implements CommandExecutor {

    private final Main plugin;

    public SpawnPassiveMobCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("Usage: /spawnPassiveMob <type>");
            return true;
        }

        String type = args[0].toLowerCase();
        
        MobControl mob = MobControlFactory.createPig(player.getWorld(), player.getLocation());
        player.sendMessage("Pig spawn !");
        return true;
    }
}