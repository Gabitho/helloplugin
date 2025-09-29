package com.pvpheads.listeners;

// BUKKIT de base
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;

// ENTITÉS
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Player;

// ÉVÉNEMENTS
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

// INVENTAIRE & ITEM
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

// COOLDOWN, TEMPS
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// VECTEUR & DIRECTION
import org.bukkit.util.Vector;

// PERSISTANCE
import org.bukkit.persistence.PersistentDataType;

// AUTRES
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

public class DragonSwordListener implements Listener {

    private final NamespacedKey key = new NamespacedKey("pvpheads", "dragon_sword");

    // Temps de recharge (en millisecondes)
    private final long cooldownTime = 5000; // 5 secondes

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Vérif cooldown
        if (cooldowns.containsKey(uuid)) {
            long lastUse = cooldowns.get(uuid);
            if (now - lastUse < cooldownTime) {
                long secondsLeft = (cooldownTime - (now - lastUse)) / 1000 +1;
                player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(
                        ChatColor.RED + "⏳ Attends encore " + secondsLeft + "s"
                    )
                );
                return;
            }
        }

        // 🐉 Souffle du dragon
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize(); // Mettre le vecteur à scale: 1

        Location spawnLoc = eye.clone().add(direction.multiply(1.2)).subtract(0,0.5,0);

        DragonFireball fireball = player.getWorld().spawn(spawnLoc, DragonFireball.class);
        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setYield(0);
        fireball.setIsIncendiary(false);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);

        // Enregistre cooldown
        long currentTime = System.currentTimeMillis();
        cooldowns.put(uuid, currentTime);

        // 🧪 Affichage du cooldown dans l’ActionBar
        new BukkitRunnable() {
            int ticksPassed = 0;
            @Override
            public void run() {
                long elapsed = ticksPassed * 50L;
                long remaining = cooldownTime - elapsed;

                if (remaining > 0) {
                    long secondsLeft = remaining / 1000;
                    player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(
                            ChatColor.LIGHT_PURPLE + "⏳ Cooldown: " + secondsLeft + "s"
                        )
                    );
                } else {
                    player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(
                            ChatColor.GREEN + "✅ Épée rechargée !"
                        )
                    );
                    this.cancel();
                }
                ticksPassed += 20; // incrémente toutes les secondes (20 ticks)
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pvpheads"), 0L, 20L); // toutes les secondes
    }
}
