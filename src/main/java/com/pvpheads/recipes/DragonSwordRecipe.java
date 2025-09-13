/**
 * Enregistre la recette vanilla (fallback simple, sans NBT exotique)
 */
private void registerRecipe() {
    // fallback : DIAMOND_SWORD simple (aucune méta spéciale ici)
    ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);

    // construit la recipe
    ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
    recipe.shape("BDB", "BHB", "BSB");
    recipe.setIngredient('B', Material.DRAGON_BREATH);
    recipe.setIngredient('D', Material.DRAGON_EGG);
    recipe.setIngredient('H', Material.PLAYER_HEAD);
    recipe.setIngredient('S', Material.DIAMOND_SWORD);

    // Avant d'ajouter : vérifie qu'il n'existe pas déjà une recette du même key
    boolean already = false;
    for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext();) {
        Recipe r = it.next();
        // si l'API expose getKey (nouvelle API), on peut comparer ; sinon on ignore
        try {
            java.lang.reflect.Method m = r.getClass().getMethod("getKey");
            Object keyObj = m.invoke(r);
            if (keyObj != null && keyObj.toString().contains(recipeKey.getKey())) {
                already = true;
                break;
            }
        } catch (Exception ex) {
            // ignore : pas grave si on ne peut pas déterminer la clé via réflexion
        }
    }

    // ADD THE RECIPE TO THE SERVER (this was missing!)
    if (!already) {
        boolean success = Bukkit.addRecipe(recipe);
        if (success) {
            plugin.getLogger().info("Dragon Sword recipe registered successfully!");
        } else {
            plugin.getLogger().warning("Failed to register Dragon Sword recipe!");
        }
    } else {
        plugin.getLogger().info("Dragon Sword recipe already exists, skipping registration.");
    }
}