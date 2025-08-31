package com.pvpheads.recipes; // ← Le dossier où est ce fichier (doit correspondre au chemin réel)

import org.bukkit.Material; // Pour choisir les objets Minecraft (épée, œuf de dragon...)
import org.bukkit.NamespacedKey; // Pour créer une clé unique dans le PDC
import org.bukkit.inventory.ItemStack; // Représente un objet Minecraft dans le code
import org.bukkit.inventory.ShapedRecipe; // Pour définir une recette avec une forme
import org.bukkit.inventory.meta.ItemMeta; // Pour modifier les propriétés visibles (nom, lore...)
import org.bukkit.persistence.PersistentDataContainer; // Pour stocker des données invisibles
import org.bukkit.persistence.PersistentDataType; // Type des données dans le PDC
import org.bukkit.plugin.Plugin; // Représente ton plugin (nécessaire pour les clés)

public class DragonSwordRecipe { // ← On déclare une classe qui contient uniquement la recette

    // Cette méthode sera appelée dans ton plugin principal (onEnable)
    public static void register(Plugin plugin) {

        // 🛠 On commence par créer une épée en diamant
        ItemStack result = new ItemStack(Material.DIAMOND_SWORD);

        // 🎨 On modifie son apparence (nom affiché)
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName("§5Épée du Dragon"); // Le §5 rend le texte violet foncé
        meta.setUnbreakable(true);
        meta.setCustom
        
        // 🧪 On crée une "clé" unique pour ce plugin, qui sera utilisée pour taguer l’objet
        NamespacedKey key = new NamespacedKey(plugin, "dragon_sword");

        // 🧬 On accède au "PersistentDataContainer" de l’objet : une zone invisible où stocker des infos
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // 🏷 On y met une info "dragon_sword" avec notre clé, pour l'identifier plus tard
        container.set(key, PersistentDataType.STRING, "dragon_sword");

        // ✔️ On applique les changements de nom + tag
        result.setItemMeta(meta);

        // 🍳 On crée une recette en forme (craft personnalisé)
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(" D ", " S ", " H "); // Forme verticale : œuf, épée, tête

        // 🔣 On définit ce que signifie chaque lettre dans la recette
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);
        recipe.setIngredient('H', Material.PLAYER_HEAD);

        // 📥 On ajoute cette recette au jeu
        plugin.getServer().addRecipe(recipe);
    }
}
