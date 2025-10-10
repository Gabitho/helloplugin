package com.pvpheads.commands;

import com.pvpheads.displaymob.DisplayMobManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Location;

/**
 * /spawnpassivemob [scale] [hp] [hitboxSize]
 * Example: /spawnpassivemob 1.1 20 3
 */
public class SpawnPassiveMobCommand implements CommandExecutor {

    private final Plugin plugin;
    private final DisplayMobManager mobManager;

    public SpawnPassiveMobCommand(Plugin plugin, DisplayMobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cmd only for players.");
            return true;
        }
        Player player = (Player) sender;

        float scale = 1.0f;
        int hp = 20;
        int hitboxSize = 3;

        try {
            if (args.length >= 1) scale = Float.parseFloat(args[0]);
            if (args.length >= 2) hp = Integer.parseInt(args[1]);
            if (args.length >= 3) hitboxSize = Integer.parseInt(args[2]);
        } catch (Exception ex) {
            player.sendMessage("Arguments invalids. Usage: /spawnpassivemob [scale] [hp] [hitboxSize]");
            return true;
        }

        Location spawnLocation = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(3));
        DisplayMobManager.DisplayMobManagerException ignored = null; // noop to keep compile friendly

        // spawn using manager
        com.pvpheads.displaymob.DisplayMob mob = mobManager.spawn(spawnLocation, scale, hitboxSize);
        player.sendMessage("Spawned passive DisplayMob id=" + mob.getId());
        return true;
    }
}
