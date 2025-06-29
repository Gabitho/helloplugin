package com.pvpheads.recipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class DragonSwordRecipe {

    public static void register(Plugin plugin) {
        // 1. Crée l'objet final : une épée personnalisée
        ItemStack result = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName("§5Épée du Dragon");
        result.setItemMeta(meta);

        // 2. Crée une clé unique pour la recette
        NamespacedKey key = new NamespacedKey(plugin, "dragon_sword");

        // 3. Définit la recette : en forme verticale
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(" D ", " S ", " H ");
        recipe.setIngredient('D', Material.DRAGON_EGG);       // œuf de dragon
        recipe.setIngredient('S', Material.DIAMOND_SWORD);    // épée en diamant
        recipe.setIngredient('H', Material.PLAYER_HEAD);      // tête de joueur

        // 4. Enregistre la recette dans le serveur
        plugin.getServer().addRecipe(recipe);
    }
}

