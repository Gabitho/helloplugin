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
 * - au CraftItemEvent : on crée directement l’item avec itemModel, PDC, et on donne l’item
 */
public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;      // clé de la recette
    private final NamespacedKey dragonSwordKey; // clé PDC plugin pour reconnaître l'item
    private final ItemStack dragonSwordTemplate; // template utilisé pour la preview

    // snapshot des inventaires avant prise du résultat (key = player UUID)
    private final Map<UUID, ItemStack[]> inventorySnapshots = new HashMap<>();

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
     * - PDC serveur pour marquer l’item
     * - setItemModel via NamespacedKey pour le modèle côté client/pack
     */
    private ItemStack createDragonSwordTemplate() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");

            // PDC côté serveur
            meta.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");

            // la nouveauté : setItemModel pour utiliser le modèle déclaré dans le pack
            NamespacedKey modelKey = new NamespacedKey("pvpheads", "dragon_sword");
            meta.setItemModel(modelKey);

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

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
        if (!shaped.getKey().equals(recipeKey)) return;

        if (!(event.getView().getPlayer() instanceof Player)) return;
        Player player = (Player) event.getView().getPlayer();

        // snapshot de l’inventaire actuel
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) copy[i] = (contents[i] == null) ? null : contents[i].clone();
        inventorySnapshots.put(player.getUniqueId(), copy);

        // remplace l’aperçu par notre template
        event.getInventory().setResult(dragonSwordTemplate.clone());
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe() == null) return;
        if (!(event.getRecipe() instanceof ShapedRecipe shaped)) return;
        if (!shaped.getKey().equals(recipeKey)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Annule le transfert vanilla : on va gérer nous‑même
        event.setCancelled(true);

        // Consommation manuelle de la matrice de craft (force la consommation de l’épée ingrédient)
        try {
            CraftingInventory inv = (CraftingInventory) event.getInventory();
            ItemStack[] matrix = inv.getMatrix();
            for (int i = 0; i < matrix.length; i++) {
                ItemStack it = matrix[i];
                if (it != null && it.getType() == Material.DIAMOND_SWORD) {
                    matrix[i] = null;
                    break;
                }
            }
            inv.setMatrix(matrix);
        } catch (Exception ex) {
            plugin.getLogger().warning("Erreur en consommant la matrice: " + ex.getMessage());
        }

        // Création de l’ItemStack spécial pour le joueur
        ItemStack givenSword = dragonSwordTemplate.clone();
        player.getInventory().addItem(givenSword);
        player.updateInventory();

        // On peut retirer la snapshot & recherche complexe — plus nécessaire.

    }
}
