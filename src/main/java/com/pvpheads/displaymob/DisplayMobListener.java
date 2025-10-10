package com.pvpheads.displaymob;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Listener : capture les hits sur nos slimes-hitbox et redirige vers DisplayMob.applyDamage(...)
 */
public class DisplayMobListener implements Listener {

    private final Plugin plugin;
    private final NamespacedKey pdcKey;
    private final DisplayMobManager mobManager;

    public DisplayMobListener(Plugin plugin, DisplayMobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.pdcKey = new NamespacedKey(plugin, "display_mob_id");
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity targetEntity = event.getEntity();

        if (!(targetEntity instanceof Slime)) return;
        Slime hitbox = (Slime) targetEntity;

        if (!hitbox.getPersistentDataContainer().has(pdcKey, PersistentDataType.STRING)) return;
        String mobIdString = hitbox.getPersistentDataContainer().get(pdcKey, PersistentDataType.STRING);
        if (mobIdString == null) return;

        // Cancel vanilla damage so we control HP/drops
        event.setCancelled(true);

        UUID mobUuid;
        try {
            mobUuid = UUID.fromString(mobIdString);
        } catch (Exception ex) {
            plugin.getLogger().warning("DisplayMobListener: invalid UUID in PDC: " + mobIdString);
            return;
        }

        DisplayMob mob = mobManager.get(mobUuid);
        if (mob == null) {
            plugin.getLogger().warning("DisplayMobListener: display mob not found for id " + mobUuid);
            return;
        }

        // convert damage to int (floor) and apply to mob
        int damageAmount = Math.max(1, (int) Math.floor(event.getDamage()));
        mob.applyDamage(damageAmount, event.getDamager());
    }
}
