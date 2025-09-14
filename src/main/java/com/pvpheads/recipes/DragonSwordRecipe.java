package com.pvpheads.recipes;

import com.pvpheads.Main; // adapte si ta classe principale a un autre nom
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.entity.Player;
import org.bukkit.entity.Item; // entité item (drop)
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.World;
import org.bukkit.Location;

/**
 * DragonSwordRecipe (version /give then PDC)
 */
public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;      // clé unique pour la recette plugin
    private final NamespacedKey dragonSwordKey; // clé PDC pour l'item (plugin namespace)
    private final ItemStack dragonSwordTemplate; // template clonable (meta déjà préparée)

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;

        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");

        // template utilisé uniquement pour l'aperçu (nom + apparence minimale)
        this.dragonSwordTemplate = createDragonSwordTemplate();

        registerRecipe();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("DragonSwordRecipe initialised (key=" + recipeKey + ")");
    }

    private ItemStack createDragonSwordTemplate() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");
            // NOTE: Nous **n'écrivons pas** ici le PDC de custom_model_data qui n'est pas pris en charge
            // uniformément par l'API pour les strings — on donnera l'item via /give pour le string CMD.
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private void registerRecipe() {
        ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
        recipe.shape("BDB", "BHB", "BSB");
        recipe.setIngredient('B', Material.DRAGON_BREATH);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        // évite d'ajouter en double — simple scan des recettes existantes
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
            try {
                Bukkit.addRecipe(recipe);
                plugin.getLogger().info("Added DragonSword recipe successfully.");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to add DragonSword recipe: " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("DragonSword recipe key already present; not adding.");
        }
    }

    /**
     * Preview replacement -> le joueur voit bien l'épée custom dans la table
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {
            // on met le template pour l'aperçu (nom/texture visible côté client si RP actif)
            event.getInventory().setResult(dragonSwordTemplate.clone());
        }
    }

    /**
     * Quand le joueur récupère l'item crafté, on:
     * 1) attend 1 tick
     * 2) supprime l'épée craftée (vanilla) de l'inventaire si elle existe
     * 3) exécute /give ... with custom_model_data string
     * 4) après 1 tick, cherche l'item donné (inv ou drop) et applique le PDC plugin
     */
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        // ne pas bloquer la mécanique du craft (on laisse consommer les ingrédients)
        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {

            Player player = (Player) event.getWhoClicked();

            // Delay 1 tick pour laisser le serveur déposer l'item dans l'inventaire
            new BukkitRunnable() {
                @Override
                public void run() {
                    // 1) supprimer une épée craftée (vanilla) si présente
                    boolean removed = removeOneCraftedPreviewSwordFromInventory(player);

                    // 2) exécute la commande /give vanilla qui met custom_model_data string
                    String modelString = "dragon_sword";
                    String jsonName = "{\"text\":\"Épée du Dragon\",\"color\":\"light_purple\"}";
                    String cmd = String.format(
                        "give %s diamond_sword[minecraft:custom_model_data={strings:[\"%s\"]},display:{Name:'%s'}] 1",
                        player.getName().replace(" ", ""),
                        modelString,
                        jsonName
                    );
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

                    // 3) attend encore 1 tick puis applique le PDC sur l'item reçu
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            boolean applied = applyPdcToGivenSword(player);
                            if (!applied) {
                                plugin.getLogger().warning("Could not find given dragon sword to apply PDC for " + player.getName());
                            }
                        }
                    }.runTaskLater(plugin, 1L);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    /* ----------------------
       Helpers
       ---------------------- */

    // Enlève une épée preview (celle qui porte le display name "Épée du Dragon") si présente,
    // retourne true si une épée a été retirée.
    private boolean removeOneCraftedPreviewSwordFromInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (isPreviewDragonSword(it)) {
                int amount = it.getAmount();
                if (amount > 1) {
                    it.setAmount(amount - 1);
                    player.getInventory().setItem(i, it);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    // Applique le PDC plugin sur la première épée 'donnée' détectée (inventaire ou drop).
    private boolean applyPdcToGivenSword(Player player) {
        // Cherche dans l'inventaire
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (isGivenDragonSword(it)) {
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");
                    it.setItemMeta(meta);
                    player.getInventory().setItem(i, it);
                    return true;
                }
            }
        }

        // Si non trouvé dans l'inventaire : chercher un drop près du joueur (s'il y a eu overflow)
        World w = player.getWorld();
        Location loc = player.getLocation();
        double radius = 3.0;
        for (Entity e : w.getNearbyEntities(loc, radius, radius, radius)) {
            if (e instanceof Item dropped) {
                ItemStack stack = dropped.getItemStack();
                if (isGivenDragonSword(stack)) {
                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(dragonSwordKey, PersistentDataType.STRING, "true");
                        stack.setItemMeta(meta);
                        dropped.setItemStack(stack);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Détermine si l'ItemStack est la preview (a le nom affiché "Épée du Dragon")
    private boolean isPreviewDragonSword(ItemStack it) {
        if (it == null) return false;
        if (it.getType() != Material.DIAMOND_SWORD) return false;
        if (!it.hasItemMeta()) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null || !m.hasDisplayName()) return false;
        return ChatColor.stripColor(m.getDisplayName()).equals("Épée du Dragon");
    }

    // Détermine si l'ItemStack correspond à l'objet qui a été donné par /give (vérifie displayName)
    // (on pourrait aussi vérifier d'autres indices)
    private boolean isGivenDragonSword(ItemStack it) {
        // Ici on test le displayName (le /give l'a mis). Tu peux l'adapter (vérifier lore etc.)
        return isPreviewDragonSword(it);
    }
}
