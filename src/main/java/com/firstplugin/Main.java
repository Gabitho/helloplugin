package com.firstplugin;

import com.firstplugin.commands.givetntlauncher;
import com.firstplugin.listeners.tntshoot;
import com.firstplugin.weapons.TNTLauncher;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private TNTLauncher launcher;

    @Override
    public void onEnable() {
        launcher = new TNTLauncher(this);

        getCommand("gettnt").setExecutor(new givetntlauncher(launcher));
        getServer().getPluginManager().registerEvents(new tntshoot(this), this);
    }

    public TNTLauncher getLauncher() {
        return launcher;
    }
}
