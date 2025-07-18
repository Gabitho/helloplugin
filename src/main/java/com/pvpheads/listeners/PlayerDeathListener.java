package com.pvpheads.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent; // ✅ Manquant
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta; // ✅ Manquant


public class PlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return; // Mort naturelle ou par environnement
        if (killer.getUniqueId().equals(victim.getUniqueId())) return; // Se tuer soi-même => refusé


        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(victim);
        meta.setDisplayName("§cTête de " + victim.getName());
        skull.setItemMeta(meta);

        victim.getWorld().dropItemNaturally(victim.getLocation(), skull);
    }
}
