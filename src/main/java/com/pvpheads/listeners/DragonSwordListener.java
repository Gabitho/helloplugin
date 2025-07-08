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
    if (cooldowns.containsKey(uuid)) {
        long lastUse = cooldowns.get(uuid);
        if (now - lastUse < cooldownTime) {
            long secondsLeft = (cooldownTime - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.RED + "❌ Attends encore " + secondsLeft + " secondes !");
            return;
        }
    }

    // 🐉 Souffle du dragon
    DragonFireball fireball = player.launchProjectile(DragonFireball.class);
    fireball.setDirection(player.getLocation().getDirection());
    fireball.setYield(0);
    fireball.setIsIncendiary(false);
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);

    // 🧪 Simulation cooldown visuel via la barre de durabilité
    int maxDurability = item.getType().getMaxDurability();
    ItemStack finalItem = item;

    // On met l’épée à durabilité "cassée"
    if (finalItem.getItemMeta() instanceof Damageable) {
        Damageable damageMeta = (Damageable) finalItem.getItemMeta();
        damageMeta.setDamage(maxDurability);
        finalItem.setItemMeta((ItemMeta) damageMeta);
    }

    // Tâche Bukkit pour recharger la barre progressivement
    new BukkitRunnable() {
        int ticksPassed = 0;
        @Override
        public void run() {
            ticksPassed += 2; // toutes les 2 ticks
            int progress = (int) ((1 - (ticksPassed / (double) cooldownTicks)) * maxDurability);

            if (progress < 0) progress = 0;

            if (finalItem.getItemMeta() instanceof Damageable) {
                Damageable meta = (Damageable) finalItem.getItemMeta();
                meta.setDamage(progress);
                finalItem.setItemMeta((ItemMeta) meta);
            }

            if (ticksPassed >= cooldownTicks) {
                this.cancel();
            }
        }
    }.runTaskTimer(Bukkit.getPluginManager().getPlugin("pvpheads"), 0L, 2L); // exécute toutes les 2 ticks

    cooldowns.put(uuid, now);
}
