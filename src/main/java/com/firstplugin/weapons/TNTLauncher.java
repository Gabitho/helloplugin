package com.firstplugin.weapons;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class TNTLauncher {

    private final JavaPlugin plugin;

    public TNTLauncher(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack create() {
        ItemStack arc = new ItemStack(Material.BOW);
        ItemMeta meta = arc.getItemMeta();

        if (meta == null) return arc;

        meta.setDisplayName("§cLance-TNT");
        NamespacedKey weaponKey = new NamespacedKey(plugin, "tnt_launcher");
        meta.getPersistentDataContainer().set(
        weaponKey,
        PersistentDataType.STRING,
        "tnt_launcher"
    );

        arc.setItemMeta(meta);
        return arc;
    }
}
