// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import idleSorcererV2.data.BaseEnchant;
import idleSorcererV2.data.EnchantInstance;
import idleSorcererV2.data.PlayerSpell;
import idleSorvererV2.enums.DamageType;
import idleSorvererV2.enums.EnchantEffectType;
import idleSorvererV2.enums.EnchantTargetParameter;
import idleSorvererV2.enums.HealingType;
import idleSorvererV2.enums.PrimaryAttributeType;

public class Player implements Combatant<PlayerSpell> {

    private final String name;
    private final Stats stats; // Manages primary attributes and their direct derived bonuses
    private final Inventory inventory;

    public static final int NUM_ACTIVE_SPELL_SLOTS = 2;
    public static final int NUM_PASSIVE_SPELL_SLOTS = 4;
    private final PlayerSpell[] activeSpells;
    private final PlayerSpell[] passiveSpells;
    private final Map<PlayerSpell, Double> activeSpellCooldowns; // Tracks cooldowns for equipped active spells

    private int currentHP;
    private int currentShield; // Player's current shield points
    private int currentPoisonDpsTaken; // Poison DPS currently affecting the player

    // Temporary combat buffs/debuffs (values are deltas from base)
    private int temporaryArmorBonus;
    private int temporaryDodgeBonus;
    private int temporaryAccuracyBonus;
    private int temporaryRegenBonus;

    private int attributePoints; // AP currency
    private CombatMode combatMode;

    private static final int BASE_PLAYER_HP = 30; // Player's starting HP before any stats

    public enum CombatMode {
        ADVANCE,
        FARMING
    }

    public Player(String name) {
        this.name = name;
        this.stats = new Stats(); // Player starts with 0 in all primary attributes
        this.inventory = new Inventory();
        this.activeSpells = new PlayerSpell[NUM_ACTIVE_SPELL_SLOTS];
        this.passiveSpells = new PlayerSpell[NUM_PASSIVE_SPELL_SLOTS];
        this.activeSpellCooldowns = new HashMap<>();
        this.attributePoints = 0; // Starting AP
        this.combatMode = CombatMode.FARMING; // Default mode
        resetCombatState(); // Initialize HP and other combat stats
    }

    // --- Combatant Interface Implementation ---

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isAlive() {
        return currentHP > 0;
    }

    @Override
    public int getCurrentHP() {
        return currentHP;
    }

    @Override
    public int getMaxHP() {
        double base = BASE_PLAYER_HP;
        double flatBonusFromAttributes = stats.getFlatHpBonusFromTotalAttributes();
        double percentBonusFromFortitude = stats.getCalculatedMaxHealthBonusPercent(); // From Fortitude in Stats

        double currentMaxHP = base + flatBonusFromAttributes;
        currentMaxHP *= (1.0 + (percentBonusFromFortitude / 100.0));

        // Add bonuses from equipped spell enchants
        for (PlayerSpell spell : getAllEquippedSpells()) {
            if (spell != null) {
                for (EnchantInstance enchant : spell.appliedEnchants()) {
                    BaseEnchant be = enchant.baseEnchant(); // Get the BaseEnchant
                    if (be.effectType() == EnchantEffectType.PLAYER_SECONDARY_STAT_ADD_FLAT &&
                        be.targetKey().equalsIgnoreCase(EnchantTargetParameter.MAX_HP.name())) {
                        currentMaxHP += enchant.finalRolledValue();
                    }
                }
            }
        }
        return Math.max(1, (int) Math.round(currentMaxHP));
    }

    @Override
    public int getCurrentShield() {
        return currentShield;
    }

    @Override
    public int getEffectiveArmor() {
        int armorFromStatsAndEnchants = (int) Math.round(stats.getArmor()); // Base armor from Stats
        // Add bonuses from equipped spell enchants
        for (PlayerSpell spell : getAllEquippedSpells()) {
            if (spell != null) {
                for (EnchantInstance enchant : spell.appliedEnchants()) {
                    BaseEnchant be = enchant.baseEnchant(); // Get the BaseEnchant
                    if (be.effectType() == EnchantEffectType.PLAYER_SECONDARY_STAT_ADD_FLAT &&
                        be.targetKey().equalsIgnoreCase(EnchantTargetParameter.ARMOR.name())) {
                        armorFromStatsAndEnchants += (int) enchant.finalRolledValue();
                    }
                }
            }
        }
        return Math.max(0, armorFromStatsAndEnchants + temporaryArmorBonus);
    }

    @Override
    public int getEffectiveDodge() {
        int dodgeFromStatsAndEnchants = (int) Math.round(stats.getCalculatedDodgePercent()); // Base from Agility
         for (PlayerSpell spell : getAllEquippedSpells()) {
             if (spell != null) { // Assuming 'spell' is defined in an outer loop iterating through equipped spells
                 for (EnchantInstance enchant : spell.appliedEnchants()) {
                     BaseEnchant be = enchant.baseEnchant(); // Get the BaseEnchant for clarity
                     if (be.effectType() == EnchantEffectType.PLAYER_SECONDARY_STAT_ADD_PERCENT &&
                         be.targetKey() != null &&
                         be.targetKey().equalsIgnoreCase(EnchantTargetParameter.DODGE_CHANCE_PERCENT.name())) {
                     }
                 }
             }
        }
        return Math.max(0, dodgeFromStatsAndEnchants + temporaryDodgeBonus);
    }

    public Stats getStats() { return stats; }

    @Override
    public int getEffectiveAccuracy() {
        int accuracyFromStats = (int) Math.round(stats.getCalculatedAccuracyPercent()); // Base from Dexterity in Stats
        double globalEnchantBonus = 0;
        for (PlayerSpell spell : getAllEquippedSpells()) {
            if (spell != null) {
                for (EnchantInstance enchant : spell.appliedEnchants()) {
                    BaseEnchant be = enchant.baseEnchant();
                    if (be.effectType() == EnchantEffectType.PLAYER_SECONDARY_STAT_ADD_PERCENT &&
                        be.targetKey() != null &&
                        be.targetKey().equals(EnchantTargetParameter.GLOBAL_ACCURACY_PERCENT.name())) {
                        globalEnchantBonus += enchant.finalRolledValue();
                    }
                }
            }
        }
        accuracyFromStats = (int) Math.round(accuracyFromStats * (1.0 + (globalEnchantBonus / 100.0)));

        return Math.max(0, accuracyFromStats + temporaryAccuracyBonus);
    }

    @Override
    public int getEffectiveRegenPerSecond() {
        int regenFromStatsAndEnchants = (int) Math.round(stats.getRegenPerSecond());
        for (PlayerSpell spell : getAllEquippedSpells()) {
            if (spell != null) { // Assuming 'spell' is defined in an outer loop
                for (EnchantInstance enchant : spell.appliedEnchants()) {
                    BaseEnchant be = enchant.baseEnchant(); // Get the BaseEnchant
                    if (be.effectType() == EnchantEffectType.PLAYER_SECONDARY_STAT_ADD_FLAT &&
                        be.targetKey() != null &&
                        be.targetKey().equalsIgnoreCase(EnchantTargetParameter.HP_REGEN_PER_SEC.name())) {
                    }
                }
            }
        }
        return regenFromStatsAndEnchants + temporaryRegenBonus; // Can be negative if debuffed heavily
    }

    @Override
    public int getCurrentPoisonTakenPerSecond() {
        return currentPoisonDpsTaken;
    }

    @Override
    public void takeDamage(int rawDamage, DamageType type, boolean isArmorPiercing, boolean isShieldPiercing, int attackerAccuracy) {
        if (!isAlive()) {
			return;
		}

        // 1. Dodge Calculation
        int effectiveHitChance = Math.max(5, Math.min(100, attackerAccuracy - getEffectiveDodge())); // Clamp between 5% and 100% hit chance
        if (new Random().nextInt(100) + 1 > effectiveHitChance) {
            System.out.println(getName() + " dodged the attack!"); // Or log to combat display
            return;
        }

        int damageAfterArmor = 0;
        if (!isArmorPiercing) {
            damageAfterArmor = Math.max(0, rawDamage - getEffectiveArmor());
        }

        int damageToHP = 0;
        if (isShieldPiercing || this.currentShield <= 0) {
            damageToHP = damageAfterArmor;
        } else {
            int damageAbsorbedByShield = Math.min(damageAfterArmor, this.currentShield);
            this.currentShield -= damageAbsorbedByShield;
            damageToHP = damageAfterArmor - damageAbsorbedByShield;
        }

        this.currentHP -= damageToHP;
        if (this.currentHP < 0) {
            this.currentHP = 0;
        }
        System.out.println(getName() + " took " + damageToHP + " damage. HP: " + currentHP + "/" + getMaxHP()); // Logging
    }

    @Override
    public void applyHealing(int amount) {
        if (!isAlive() || amount <= 0) {
			return;
		}
        this.currentHP = Math.min(getMaxHP(), this.currentHP + amount);
        System.out.println(getName() + " healed for " + amount + ". HP: " + currentHP + "/" + getMaxHP());
    }

    @Override
    public void applyShield(int amount) {
        if (amount <= 0) {
			return;
		}
        this.currentShield += amount;
        System.out.println(getName() + " gained " + amount + " shield. Current Shield: " + currentShield);
    }

    @Override
    public void modifyTemporaryArmor(int amountDelta) { this.temporaryArmorBonus += amountDelta; }
    @Override
    public void modifyTemporaryRegenPerSecond(int amountDelta) { this.temporaryRegenBonus += amountDelta; }
    @Override
    public void modifyTemporaryDodge(int amountDelta) { this.temporaryDodgeBonus += amountDelta; }
    @Override
    public void modifyTemporaryAccuracy(int amountDelta) { this.temporaryAccuracyBonus += amountDelta; }

    @Override
    public void applyPoisonDamagePerSecond(int dpsAmount) {
        this.currentPoisonDpsTaken += dpsAmount;
        if (this.currentPoisonDpsTaken < 0)
		 {
			this.currentPoisonDpsTaken = 0; // Cannot have negative poison
		}
    }

    @Override
    public void clearTemporaryCombatEffectsAndPoison() {
        this.currentPoisonDpsTaken = 0;
        this.temporaryArmorBonus = 0;
        this.temporaryDodgeBonus = 0;
        this.temporaryAccuracyBonus = 0;
        this.temporaryRegenBonus = 0;
        // Note: currentShield might persist or reset based on game rules. Resetting here.
        // this.currentShield = 0; // Or reset to a base if player has innate shield
    }

    @Override
    public void resetCombatState() {
        this.currentHP = getMaxHP(); // Full HP
        this.currentShield = 0;      // Reset shield, player earns it via spells
        clearTemporaryCombatEffectsAndPoison();

        activeSpellCooldowns.clear();
        for (PlayerSpell spell : activeSpells) {
            if (spell != null) {
                activeSpellCooldowns.put(spell, spell.actualCooldownSeconds()); // Start on full CD
            }
        }
        System.out.println(getName() + " combat state reset. HP: " + currentHP + "/" + getMaxHP());
    }

    @Override
    public void updateCooldowns(double deltaTime) {
        // Iterate over a copy of keys to avoid ConcurrentModificationException if a spell were unequipped during iteration
        List<PlayerSpell> spellsToUpdate = new ArrayList<>(activeSpellCooldowns.keySet());
        for (PlayerSpell spell : spellsToUpdate) {
            if (activeSpellCooldowns.containsKey(spell)) { // Check if still equipped
                double currentCD = activeSpellCooldowns.get(spell);
                if (currentCD > 0) {
                    currentCD -= deltaTime;
                    activeSpellCooldowns.put(spell, Math.max(0, currentCD));
                }
            }
        }
    }

    @Override
    public List<PlayerSpell> getReadySpells() {
        List<PlayerSpell> readySpells = new ArrayList<>();
        for (PlayerSpell spell : activeSpells) {
            if (spell != null && activeSpellCooldowns.getOrDefault(spell, 0.0) <= 0) {
                readySpells.add(spell);
            }
        }
        return readySpells;
    }

    @Override
    public void triggerCooldownForSpell(PlayerSpell spell) {
        if (spell != null && Arrays.asList(activeSpells).contains(spell)) {
            activeSpellCooldowns.put(spell, spell.actualCooldownSeconds());
        }
    }

    @Override
    public void applyPeriodicEffects() {
        if (!isAlive()) {
			return;
		}

        int netHPSChange = getEffectiveRegenPerSecond() - this.currentPoisonDpsTaken;

        if (netHPSChange > 0) {
            applyHealing(netHPSChange);
        } else if (netHPSChange < 0) {
            // Taking damage from poison (netted with regen)
            // This damage should bypass shield and armor as it's internal.
            this.currentHP += netHPSChange; // netHPSChange is negative here
            System.out.println(getName() + " took " + (-netHPSChange) + " poison/degen damage. HP: " + currentHP + "/" + getMaxHP());
            if (this.currentHP < 0) {
                this.currentHP = 0;
            }
        }
    }

    // --- Inventory and Spell Management ---

    public Inventory getInventory() {
        return inventory;
    }

    public boolean addSpellToInventory(PlayerSpell spell) {
        return inventory.addSpell(spell);
    }

    /**
     * Deconstructs a spell from inventory for AP.
     * @param inventoryIndex The index of the spell in the inventory.
     * @return true if deconstruction was successful, false otherwise.
     */
    public boolean deconstructSpell(int inventoryIndex) {
        PlayerSpell spellToDeconstruct = inventory.getSpell(inventoryIndex); // Get without removing first
        if (spellToDeconstruct != null) {
            // Ensure it's not currently equipped
            for (PlayerSpell active : activeSpells) {
                if (active == spellToDeconstruct) { // Check by reference
                    System.err.println("Cannot deconstruct an equipped active spell.");
                    return false;
                }
            }
            for (PlayerSpell passive : passiveSpells) {
                if (passive == spellToDeconstruct) {
                     System.err.println("Cannot deconstruct an equipped passive spell.");
                    return false;
                }
            }

            PlayerSpell removedSpell = inventory.removeSpell(inventoryIndex); // Now actually remove
            if (removedSpell != null) { // Should be same as spellToDeconstruct
                this.attributePoints += removedSpell.finalAPCost();
                System.out.println("Deconstructed " + removedSpell.getName() + " for " + removedSpell.finalAPCost() + " AP. Total AP: " + this.attributePoints);
                return true;
            }
        }
        System.err.println("Failed to deconstruct spell at index " + inventoryIndex);
        return false;
    }

    /**
     * Equips a spell from the inventory into an active or passive slot.
     * @param inventoryIndex Index of the spell in the inventory.
     * @param slotIndex Index of the spell slot (0-1 for active, 0-3 for passive).
     * @param isActiveSlot True if equipping to an active slot, false for passive.
     * @return true if successful, false otherwise.
     */
    public boolean equipSpell(int inventoryIndex, int slotIndex, boolean isActiveSlot) {
        PlayerSpell spellToEquip = inventory.getSpell(inventoryIndex);
        if (spellToEquip == null) {
            System.err.println("Player.equipSpell: Spell not found in inventory at index " + inventoryIndex);
            return false;
        }

        PlayerSpell[] targetSlots = isActiveSlot ? activeSpells : passiveSpells;
        int maxSlotIndex = isActiveSlot ? NUM_ACTIVE_SPELL_SLOTS : NUM_PASSIVE_SPELL_SLOTS;

        if (slotIndex < 0 || slotIndex >= maxSlotIndex) {
            System.err.println("Player.equipSpell: Invalid slot index " + slotIndex + " for " + (isActiveSlot ? "active" : "passive") + " slot.");
            return false;
        }

        // Unequip any spell currently in the target slot (if any)
        if (targetSlots[slotIndex] != null) {
            unequipSpell(slotIndex, isActiveSlot); // This will add it back to inventory
        }

        // Equip the new spell
        targetSlots[slotIndex] = spellToEquip;
        // inventory.removeSpell(spellToEquip); // OPTIONAL: Remove from main inventory list if equipped spells aren't also in general inventory

        if (isActiveSlot) {
            activeSpellCooldowns.put(spellToEquip, spellToEquip.actualCooldownSeconds()); // Start on full CD
        }

        // Recalculate dependent stats (like MaxHP) might be needed if enchants affect them
        // Or, ensure getters for effective stats always re-evaluate based on equipped items.
        // For simplicity, getters like getEffectiveArmor() already iterate equipped spells.
        System.out.println("Equipped " + spellToEquip.getName() + " to " + (isActiveSlot ? "active" : "passive") + " slot " + slotIndex);
        return true;
    }

    /**
     * Unequips a spell from a slot and returns it to the main inventory.
     * @param slotIndex Index of the spell slot.
     * @param isActiveSlot True if unequipping from an active slot.
     * @return true if successful, false otherwise.
     */
    public boolean unequipSpell(int slotIndex, boolean isActiveSlot) {
        PlayerSpell[] targetSlots = isActiveSlot ? activeSpells : passiveSpells;
        int maxSlotIndex = isActiveSlot ? NUM_ACTIVE_SPELL_SLOTS : NUM_PASSIVE_SPELL_SLOTS;

        if (slotIndex < 0 || slotIndex >= maxSlotIndex || targetSlots[slotIndex] == null) {
            System.err.println("Player.unequipSpell: Invalid slot or empty slot at index " + slotIndex);
            return false;
        }

        PlayerSpell unequippedSpell = targetSlots[slotIndex];
        targetSlots[slotIndex] = null;

        if (isActiveSlot) {
            activeSpellCooldowns.remove(unequippedSpell);
        }

        // OPTIONAL: Add back to main inventory if it was removed upon equipping
        // inventory.addSpell(unequippedSpell);

        System.out.println("Unequipped " + unequippedSpell.getName() + " from " + (isActiveSlot ? "active" : "passive") + " slot " + slotIndex);
        // Recalculate stats if needed
        return true;
    }

    /**
     * Helper to get all currently equipped spells (active and passive).
     * @return A list of all equipped PlayerSpell objects.
     */
    public List<PlayerSpell> getAllEquippedSpells() {
        List<PlayerSpell> equipped = new ArrayList<>();
        for (PlayerSpell spell : activeSpells) {
            if (spell != null) {
				equipped.add(spell);
			}
        }
        for (PlayerSpell spell : passiveSpells) {
            if (spell != null) {
				equipped.add(spell);
			}
        }
        return equipped;
    }

    public PlayerSpell[] getActiveSpells() {
        return Arrays.copyOf(activeSpells, activeSpells.length); // Return a copy
    }

    public PlayerSpell[] getPassiveSpells() {
        return Arrays.copyOf(passiveSpells, passiveSpells.length); // Return a copy
    }

    public double getActiveSpellCooldown(PlayerSpell spell) {
        return activeSpellCooldowns.getOrDefault(spell, 0.0);
    }

    // --- Attribute Point (AP) Management ---
    public int getAttributePoints() {
        return attributePoints;
    }

    public boolean spendAPForAttribute(PrimaryAttributeType attributeType) {
        if (attributeType == PrimaryAttributeType.NONE) {
            System.err.println("Cannot upgrade NONE attribute type.");
            return false;
        }

        int currentAttributeValue = stats.getPrimaryAttributeValue(attributeType);
        // Cost_to_buy_next_point = 10 + 0.5 * P'^2 + 7 * P'
        double cost = 10 + (0.5 * Math.pow(currentAttributeValue, 2)) + (7 * currentAttributeValue);
        int apCost = (int) Math.round(cost);
        apCost = Math.max(1, apCost); // Ensure cost is at least 1

        if (this.attributePoints >= apCost) {
            this.attributePoints -= apCost;
            stats.addPointsToAttribute(attributeType.name(), 1); // Assumes addPointsToAttribute takes string name
            System.out.println("Upgraded " + attributeType.name() + " for " + apCost + " AP. New value: " + (currentAttributeValue + 1) + ". Remaining AP: " + this.attributePoints);
            // Max HP might change if Fortitude was upgraded or due to total attribute points, so refresh current HP
            this.currentHP = Math.min(this.currentHP, getMaxHP()); // Ensure currentHP doesn't exceed new max
            return true;
        } else {
            System.out.println("Not enough AP to upgrade " + attributeType.name() + ". Cost: " + apCost + " AP. You have: " + this.attributePoints + " AP.");
            return false;
        }
    }

    // --- Combat Mode ---
    public CombatMode getCombatMode() {
        return combatMode;
    }
    public void setCombatMode(CombatMode mode) {
        this.combatMode = mode;
        System.out.println("Player combat mode set to: " + mode);
    }

    /**
     * Calculates the total global percentage damage bonus for a specific damage type
     * from all equipped spell enchantments.
     * @param damageType The DamageType to check for bonuses.
     * @return The total additive percentage bonus (e.g., 25.0 for +25%).
     */
    public double getGlobalEnchantDamageBonusPercent(DamageType damageType) {
        if (damageType == null) {
			return 0.0;
		}
        double totalBonus = 0.0;
        for (PlayerSpell spell : getAllEquippedSpells()) { // getAllEquippedSpells() should return active and passive
            if (spell != null) {
                for (EnchantInstance enchant : spell.appliedEnchants()) {
                    BaseEnchant be = enchant.baseEnchant();
                    if (be.effectType() == EnchantEffectType.PLAYER_DAMAGE_TYPE_ADD_PERCENT) {
                        // Check if the enchant's targetKey matches the damageType's name
                        // This assumes targetKey in BaseEnchant for damage types stores the DamageType enum's .name()
                        if (be.targetKey() != null && be.targetKey().equalsIgnoreCase(damageType.name())) {
                            totalBonus += enchant.finalRolledValue();
                        }
                    }
                }
            }
        }
        return totalBonus;
    }

    /**
     * Calculates the total global percentage healing bonus for a specific healing type
     * from all equipped spell enchantments.
     * @param healingType The HealingType to check for bonuses.
     * @return The total additive percentage bonus (e.g., 15.0 for +15%).
     */
    public double getGlobalEnchantHealingBonusPercent(HealingType healingType) {
        if (healingType == null) {
			return 0.0;
		}
        double totalBonus = 0.0;
        for (PlayerSpell spell : getAllEquippedSpells()) {
            if (spell != null) {
                for (EnchantInstance enchant : spell.appliedEnchants()) {
                    BaseEnchant be = enchant.baseEnchant();
                    if (be.effectType() == EnchantEffectType.PLAYER_HEALING_TYPE_ADD_PERCENT) {
                        // Check if the enchant's targetKey matches the healingType's name
                        if (be.targetKey() != null && be.targetKey().equalsIgnoreCase(healingType.name())) {
                            totalBonus += enchant.finalRolledValue();
                        }
                    }
                }
            }
        }
        return totalBonus;
    }
}
