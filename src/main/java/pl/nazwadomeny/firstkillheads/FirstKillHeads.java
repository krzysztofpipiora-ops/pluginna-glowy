package pl.nazwadomeny.firstkillheads;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Random;

public final class FirstKillHeads extends JavaPlugin implements Listener {

    private NamespacedKey firstDeathKey;
    private NamespacedKey headEffectKey;
    private final Random random = new Random();

    @Override
    public void onEnable() {
        this.firstDeathKey = new NamespacedKey(this, "has_died_in_pvp");
        this.headEffectKey = new NamespacedKey(this, "head_effect_type");

        getServer().getPluginManager().registerEvents(this, this);

        // Pętla sprawdzająca i nakładająca stałe efekty (Siła / Regeneracja) dla graczy noszących głowy
        Bukkit.getScheduler().runTaskTimer(this, this::applyWearingEffects, 20L, 20L);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Sprawdzamy, czy gracz zginął z rąk innego gracza
        if (killer == null || killer.equals(victim)) {
            return;
        }

        PersistentDataContainer victimData = victim.getPersistentDataContainer();

        // Sprawdzamy, czy to pierwsza śmierć w PvP
        if (!victimData.has(firstDeathKey, PersistentDataType.BYTE)) {
            // Oznaczamy gracza, że już raz zginął w PvP
            victimData.set(firstDeathKey, PersistentDataType.BYTE, (byte) 1);

            // Tworzymy i dodajemy losową głowę do dropu
            ItemStack customHead = createCustomHead(victim);
            event.getDrops().add(customHead);

            victim.sendMessage(ChatColor.RED + "To była Twoja pierwsza śmierć w PvP! Przeciwnik zdobył Twoją unikalną głowę.");
            killer.sendMessage(ChatColor.GREEN + "Zdobyłeś unikalną głowę gracza " + victim.getName() + "!");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        ItemStack helmet = attacker.getInventory().getHelmet();
        if (helmet == null || helmet.getType() != Material.PLAYER_HEAD) {
            return;
        }

        SkullMeta meta = (SkullMeta) helmet.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(headEffectKey, PersistentDataType.INTEGER)) {
            Integer effectType = pdc.get(headEffectKey, PersistentDataType.INTEGER);
            
            // Efekt 3: Nakładanie trucizny na 1 sekundę przy uderzeniu
            if (effectType != null && effectType == 3) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20, 0, false, true, true));
            }
        }
    }

    private void applyWearingEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack helmet = player.getInventory().getHelmet();
            if (helmet == null || helmet.getType() != Material.PLAYER_HEAD) continue;

            SkullMeta meta = (SkullMeta) helmet.getItemMeta();
            if (meta == null) continue;

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.has(headEffectKey, PersistentDataType.INTEGER)) continue;

            Integer effectType = pdc.get(headEffectKey, PersistentDataType.INTEGER);
            if (effectType == null) continue;

            // Typ 1: Siła I
            if (effectType == 1) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 0, false, false, true));
            } 
            // Typ 2: Regeneracja II
            else if (effectType == 2) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, false, false, true));
            }
        }
    }

    private ItemStack createCustomHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);
            
            int effectType = random.nextInt(3) + 1; // Losuje 1, 2 lub 3
            meta.getPersistentDataContainer().set(headEffectKey, PersistentDataType.INTEGER, effectType);

            switch (effectType) {
                case 1 -> {
                    meta.setDisplayName(ChatColor.GOLD + "Głowa " + player.getName() + ChatColor.RED + " [Siła + 1 Serce]");
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Efekt po założeniu:",
                            ChatColor.RED + "• Siła I",
                            ChatColor.RED + "• +1 Dodatkowe Serce"
                    ));
                    addHealthAttribute(meta);
                }
                case 2 -> {
                    meta.setDisplayName(ChatColor.GOLD + "Głowa " + player.getName() + ChatColor.GREEN + " [Regeneracja II + 1 Serce]");
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Efekt po założeniu:",
                            ChatColor.GREEN + "• Regeneracja II",
                            ChatColor.GREEN + "• +1 Dodatkowe Serce"
                    ));
                    addHealthAttribute(meta);
                }
                case 3 -> {
                    meta.setDisplayName(ChatColor.GOLD + "Głowa " + player.getName() + ChatColor.DARK_GREEN + " [Trucizna przy uderzeniu]");
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Efekt po założeniu:",
                            ChatColor.DARK_GREEN + "• Nakłada Truciznę na 1s po uderzeniu wroga"
                    ));
                }
            }

            head.setItemMeta(meta);
        }

        return head;
    }

    private void addHealthAttribute(SkullMeta meta) {
        NamespacedKey healthKey = new NamespacedKey(this, "head_extra_health");
        AttributeModifier healthModifier = new AttributeModifier(
                healthKey,
                2.0, // 2.0 = 1 serce
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.HEAD
        );
        meta.addAttributeModifier(Attribute.MAX_HEALTH, healthModifier);
    }
}
