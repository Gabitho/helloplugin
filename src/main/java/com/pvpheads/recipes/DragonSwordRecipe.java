package com.pvpheads.recipes;

import com.pvpheads.Main;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import org.bukkit.Location;
import org.bukkit.World;

import org.bukkit.persistence.PersistentDataType;

/**
 * DragonSwordRecipe
 * - crée et enregistre la recette (fallback simple)
 * - intercepte le craft et donne l'épée custom via /give
 */
public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;      // clé unique pour la recette plugin
    private final NamespacedKey dragonSwordKey; // clé PDC pour l'item (plugin namespace)

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;

        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");

        // enregistre la recette (fallback = DIAMOND_SWORD)
        registerRecipe();

        // on enregistre ce listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("DragonSwordRecipe initialised (key=" + recipeKey + ")");
    }

    /**
     * Enregistre la recette vanilla (fallback simple, sans NBT exotique)
     */
    private void registerRecipe() {
        ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
        recipe.shape("BDB", "BHB", "BSB");
        recipe.setIngredient('B', Material.DRAGON_BREATH);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        boolean already = false;
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext();) {
            Recipe r = it.next();
            try {
                java.lang.reflect.Method m = r.getClass().getMethod("getKey");
                Object keyObj = m.invoke(r);
                if (keyObj != null && keyObj.toString().contains(recipeKey.getKey())) {
                    already = true;
                    break;
                }
            } catch (Exception ex) {
                // ignore
            }
        }

        if (!already) {
            Bukkit.addRecipe(recipe);
        }
    }


private final Map<UUID, ItemStack[]> inventorySnapshots = new HashMap<>();
private static final String RAW_JSON_NAME = "{\"text\":\"Épée du Dragon\",\"color\":\"light_purple\"}";

// Prepare : on prend un snapshot (appelé avant que le joueur prenne le résultat)
@EventHandler
public void onPrepareCraft(PrepareItemCraftEvent event) {
    if (event.getRecipe() == null) return;
    if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
    if (!shaped.getKey().equals(recipeKey)) return;

    if (!(event.getView().getPlayer() instanceof Player player)) return;

    // clone de l'inventaire actuel (avant que le joueur prenne le résultat)
    ItemStack[] contents = player.getInventory().getContents();
    ItemStack[] copy = new ItemStack[contents.length];
    for (int i = 0; i < contents.length; i++) {
        ItemStack it = contents[i];
        copy[i] = (it == null) ? null : it.clone();
    }
    inventorySnapshots.put(player.getUniqueId(), copy);

    // Optionnel : tu peux aussi afficher une preview ici si tu veux
}

// Craft : le joueur prend l'item, on attend 1 tick pour voir où l'item a atterri
@EventHandler
public void onCraftItem(CraftItemEvent event) {
    if (event.getRecipe() == null) return;
    if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
    if (!shaped.getKey().equals(recipeKey)) return;
    if (!(event.getWhoClicked() instanceof Player)) return;

    Player player = (Player) event.getWhoClicked();

    // on n'annule PAS (on laisse le comportement vanilla, pour que le joueur prenne l'item)
    // mais on schedule une tâche 1 tick plus tard pour repérer le slot modifié
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        UUID uid = player.getUniqueId();
        ItemStack[] before = inventorySnapshots.remove(uid);
        ItemStack[] after = player.getInventory().getContents();

        int targetSlot = findSlotDifference(before, after); // méthode définie plus bas
        if (targetSlot == -1) {
            // fallback : prend first empty ou première diamond_sword trouvée
            targetSlot = player.getInventory().firstEmpty();
            if (targetSlot == -1) {
                // inventaire plein : laisser tomber — on gérera via drop reposition
            }
        }

        // 1) on lance le /give (donne l'item avec custom_model_data string côté client)
        String giveCmd = String.format(
            "give %s diamond_sword[minecraft:custom_model_data={strings:[\"dragon_sword\"]},minecraft:custom_name='%s'] 1",
            player.getName(),
            RAW_JSON_NAME.replace("'", "\\'")
        );
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);
        final int finalSlot = targetSlot
        // 2) 1 tick après le give, on cherche l'item donné, on le déplace dans targetSlot et on applique le PDC
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Cherche item donné dans l'inventaire
            int givenIndex = findGivenSwordIndex(player);
            if (givenIndex != -1) {
                // récupère l'item donné
                ItemStack given = player.getInventory().getItem(givenIndex);
                if (given == null) return;

                // retire la donnée depuis sa position actuelle
                player.getInventory().setItem(givenIndex, null);

                // applique PDC + nom lisible
                ItemMeta meta = given.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");
                    meta.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");
                    given.setItemMeta(meta);
                }

                // place dans le slot cible (si possible)
                if (finalSlot >= 0) {
                    player.getInventory().setItem(finalSlot, given);
                } else {
                    // si pas de slot, on tente d'ajouter normalement (fallback)
                    player.getInventory().addItem(given);
                }
                player.updateInventory();
            } else {
                // Si on n'a pas trouvé dans l'inventaire : chercher les drops proches (overflow)
                World w = player.getWorld();
                Location loc = player.getLocation();
                for (Entity e : w.getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
                    if (e instanceof Item dropped) {
                        ItemStack stack = dropped.getItemStack();
                        if (stack != null && stack.getType() == Material.DIAMOND_SWORD) {
                            ItemMeta sm = stack.getItemMeta();
                            if (sm != null && RAW_JSON_NAME.equals(sm.getDisplayName())) {
                                // applique PDC
                                sm.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");
                                sm.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");
                                stack.setItemMeta(sm);
                                dropped.setItemStack(stack);
                                // si on a un slot, on peut setItem à player, sinon laisser le drop
                                if (finalSlot >= 0) {
                                    player.getInventory().setItem(finalSlot, stack);
                                    dropped.remove();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }, 1L);

    }, 1L);
}

// Compare deux arrays pour trouver le premier index modifié
private int findSlotDifference(ItemStack[] before, ItemStack[] after) {
    if (before == null) return -1;
    int len = Math.min(before.length, after.length);
    for (int i = 0; i < len; i++) {
        ItemStack b = before[i];
        ItemStack a = after[i];
        if (!itemStackEqual(b, a)) return i;
    }
    // si la différence est plus haute (inventaire plus long), on retourne -1 (fallback)
    return -1;
}

private boolean itemStackEqual(ItemStack a, ItemStack b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    if (a.getType() != b.getType()) return false;
    if (a.getAmount() != b.getAmount()) return false;
    // ne compare pas les metas complexes pour rester permissif
    return true;
}

// cherche dans l'inventaire un item qui ressemble à celui donné par /give
private int findGivenSwordIndex(Player player) {
    ItemStack[] contents = player.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
        ItemStack it = contents[i];
        if (it == null) continue;
        if (it.getType() != Material.DIAMOND_SWORD) continue;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) continue;
        // le /give laisse souvent le raw JSON comme displayName ; on le recherche en priorité
        if (meta.hasDisplayName() && RAW_JSON_NAME.equals(meta.getDisplayName())) {
            return i;
        }
    }
    // fallback : trouve première diamond_sword sans notre PDC
    for (int i = 0; i < contents.length; i++) {
        ItemStack it = contents[i];
        if (it == null) continue;
        if (it.getType() != Material.DIAMOND_SWORD) continue;
        ItemMeta meta = it.getItemMeta();
        if (meta == null) continue;
        if (!meta.getPersistentDataContainer().has(dragonSwordKey, PersistentDataType.STRING)) {
            return i;
        }
    }
    return -1;
}


}
