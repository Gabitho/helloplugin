package com.pvpheads.recipes;

import com.pvpheads.Main;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;

import org.bukkit.persistence.PersistentDataType;

/**
 * DragonSwordRecipe
 * - enregistre la recette (fallback diamond_sword)
 * - prépare un template (preview)
 * - snapshot l'inventaire au PrepareItemCraftEvent
 * - au CraftItemEvent : détecte le slot modifié, exécute /give (string custom_model_data),
 *   applique le PDC serveur sur l'item donné, remet l'item au même slot et consomme l'ingrédient épée.
 */
public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;      // clé de la recette
    private final NamespacedKey dragonSwordKey; // clé PDC plugin pour reconnaître l'item
    private final ItemStack dragonSwordTemplate; // template utilisé pour la preview

    // snapshot des inventaires avant prise du résultat (key = player UUID)
    private final Map<UUID, ItemStack[]> inventorySnapshots = new HashMap<>();

    // raw JSON utilisé par /give (on cherchera ce raw JSON en priorité dans l'inventaire)
    private static final String RAW_JSON_NAME = "{\"text\":\"Épée du Dragon\",\"color\":\"light_purple\"}";

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");
        this.dragonSwordTemplate = createDragonSwordTemplate();

        registerRecipe();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("DragonSwordRecipe initialisé (key=" + recipeKey + ")");
    }

    /**
     * Crée l'ItemStack template pour la preview.
     * - displayName lisible
     * - PDC serveur (optionnel ici pour la preview)
     * - (optionnel) customModelData int si tu veux fallback preview
     */
    private ItemStack createDragonSwordTemplate() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");

            // PDC côté serveur (utile si tu veux détecter la preview comme item spécial côté serveur)
            meta.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");

            // Optionnel : fallback numeric CMD pour preview client si besoin
            // meta.setCustomModelData(1);

            sword.setItemMeta(meta);
        }
        return sword;
    }

    /**
     * Enregistre la recette fallback (aucune NBT exotique dans la recette).
     */
    private void registerRecipe() {
        ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
        recipe.shape("BDB", "BHB", "BSB");
        recipe.setIngredient('B', Material.DRAGON_BREATH);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        // anti-duplicate: vérifie si la clé existe déjà
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
            } catch (Exception ignored) {}
        }

        if (!already) {
            Bukkit.addRecipe(recipe);
            plugin.getLogger().info("Recette DragonSword ajoutée.");
        } else {
            plugin.getLogger().warning("Recette DragonSword déjà présente - non ajoutée.");
        }
    }

    /**
     * PrepareItemCraftEvent:
     * - si c'est notre recette, on snapshote l'inventaire du joueur (avant qu'il prenne le résultat)
     * - on place la preview (dragonSwordTemplate) dans le slot résultat
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
        if (!shaped.getKey().equals(recipeKey)) return;

        if (!(event.getView().getPlayer() instanceof Player)) return;
        Player player = (Player) event.getView().getPlayer();

        // snapshot de l'inventaire actuel (clone profond des ItemStack)
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) copy[i] = (contents[i] == null) ? null : contents[i].clone();
        inventorySnapshots.put(player.getUniqueId(), copy);

        // remplace l'aperçu par notre template (le joueur voit l'épée custom dans la table)
        event.getInventory().setResult(dragonSwordTemplate.clone());
    }

    /**
     * CraftItemEvent:
     * - on ne cancelle pas l'event pour garder le comportement vanilla (le joueur prend l'item)
     * - on attend 1 tick, on calcule le slot modifié (targetSlot)
     * - on supprime l'épée ingrédient de la grille (consommation forcée)
     * - on exécute /give avec custom_model_data string pour donner l'item retexturisé
     * - 1 tick après le /give on cherche l'item reçu, on lui applique le PDC serveur et on le replace dans targetSlot
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe() == null) return;
        if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
        if (!shaped.getKey().equals(recipeKey)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID uid = player.getUniqueId();

        // Annule le transfert vanilla : on va tout gérer nous-même
        event.setCancelled(true);

        // Optionnel : retire la preview immédiatement côté inventaire de la table (sécurité)
        try {
            event.getInventory().setResult(null);
        } catch (Exception ignored) {}

        // Consommation manuelle de la matrice de craft (force la consommation de l'épée)
        try {
            CraftingInventory inv = (CraftingInventory) event.getInventory();
            ItemStack[] matrix = inv.getMatrix();
            for (int i = 0; i < matrix.length; i++) {
                ItemStack it = matrix[i];
                if (it != null && it.getType() == Material.DIAMOND_SWORD) {
                    matrix[i] = null; // consomme l'ingrédient
                    break;
                }
            }
            inv.setMatrix(matrix);
        } catch (Exception ex) {
            plugin.getLogger().warning("Erreur en consommant la matrice: " + ex.getMessage());
        }

        // Donne l'item client-side avec custom_model_data (string) immédiatement
        String giveCmd = String.format(
            "give %s diamond_sword[minecraft:custom_model_data={strings:[\"dragon_sword\"]},minecraft:custom_name='%s'] 1",
            player.getName().replace(" ", ""),
            "{\"text\":\"Épée du Dragon\",\"color\":\"light_purple\"}".replace("'", "\\'")
        );
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);

        // Appliquer PDC serveur sur l'item réellement donné 1 tick plus tard
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Recherche de l'épée donnée dans l'inventaire
            int givenIndex = findGivenSwordIndex(player);

            if (givenIndex != -1) {
                ItemStack given = player.getInventory().getItem(givenIndex);
                if (given != null && given.getType() == Material.DIAMOND_SWORD) {
                    ItemMeta gmeta = given.getItemMeta();
                    if (gmeta != null) {
                        gmeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");
                        gmeta.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");
                        given.setItemMeta(gmeta);
                    }

                    // Si tu veux forcer le placement dans un slot particulier, fais-le ici.
                    // Exemple : on laisse tel quel (on n'essaie pas de déplacer si trouvé ailleurs).
                    player.updateInventory();
                    return;
                }
            }

            // Si on n'a pas trouvé dans l'inventaire, cherche un drop proche (overflow)
            boolean applied = false;
            World w = player.getWorld();
            Location loc = player.getLocation();
            for (Entity e : w.getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
                if (!(e instanceof Item)) continue;
                Item dropped = (Item) e;
                ItemStack stack = dropped.getItemStack();
                if (stack == null || stack.getType() != Material.DIAMOND_SWORD) continue;
                ItemMeta sm = stack.getItemMeta();
                if (sm == null) continue;
                // heuristique : s'il contient notre nom JSON brut, on l'identifie
                if (sm.hasDisplayName() && sm.getDisplayName().contains("Épée du Dragon")) {
                    sm.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");
                    sm.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");
                    stack.setItemMeta(sm);
                    dropped.setItemStack(stack);

                    applied = true;
                    break;
                }
            }

            if (!applied) {
                plugin.getLogger().warning("pvpheads: n'a pas trouvé l'épée donnée pour " + player.getName() + " (possible overflow/ timing).");
            }
        }, 1L);
    }

    /* ----------------------
       Helpers
       ---------------------- */

    /**
     * Compare deux snapshots d'inventaire et retourne le premier index modifié,
     * ou -1 si non trouvé.
     */
    private int findSlotDifference(ItemStack[] before, ItemStack[] after) {
        if (before == null || after == null) return -1;
        int len = Math.min(before.length, after.length);
        for (int i = 0; i < len; i++) {
            ItemStack b = before[i];
            ItemStack a = after[i];
            if (!itemStackEqual(b, a)) return i;
        }
        return -1;
    }

    private boolean itemStackEqual(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        if (a.getAmount() != b.getAmount()) return false;
        return true;
    }

    /**
     * Cherche dans l'inventaire la diamond_sword donnée par /give.
     * Priorise la sword dont le displayName est le JSON brut (cas fréquent),
     * sinon retourne la première diamond_sword qui n'a pas encore notre PDC.
     */
    private int findGivenSwordIndex(Player player) {
        ItemStack[] contents = player.getInventory().getContents();

        // 1) priorité : nom JSON brut
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            if (it.getType() != Material.DIAMOND_SWORD) continue;
            ItemMeta meta = it.getItemMeta();
            if (meta == null) continue;
            if (meta.hasDisplayName() && RAW_JSON_NAME.equals(meta.getDisplayName())) return i;
        }

        // 2) fallback : première DIAMOND_SWORD sans notre PDC
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            if (it.getType() != Material.DIAMOND_SWORD) continue;
            ItemMeta meta = it.getItemMeta();
            if (meta == null) continue;
            if (!meta.getPersistentDataContainer().has(dragonSwordKey, PersistentDataType.STRING)) return i;
        }

        return -1;
    }
}
