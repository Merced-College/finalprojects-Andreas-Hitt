// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import idleSorvererV2.enums.DamageType;


public class Enemy implements Combatant<EnemySpellData> {

    // Fields from JSON definition (base stats)
    private final String id;
    private final String name;
    private final int maxHP;
    private final int baseAccuracy;
    private final int baseDodge;
    private final int baseArmor;
    private final int initialShield; // Shield enemy starts combat with
    private final int baseRegenPerSecond;
    private final List<EnemySpellData> definedSpells; // Max 2 spells

    // Runtime combat stats (mutable)
    private int currentHP;
    private int currentShield;
    private int currentAccuracy; // Can be modified by player debuffs
    private int currentDodge;    // Can be modified by player debuffs
    private int currentArmor;    // Can be modified by player debuffs
    private int currentRegenPerSecond; // Can be modified by player debuffs
    private int currentPoisonDpsTaken; // Poison applied by the player

    private final double[] spellCurrentCooldowns; // Tracks cooldowns for its definedSpells

    private final Random random = new Random(); // For basic spell choice if multiple are ready

    /**
     * Constructor for Enemy.
     * @param id The unique ID of the enemy type.
     * @param name The display name of the enemy.
     * @param maxHP Maximum health points.
     * @param baseAccuracy Base accuracy rating.
     * @param baseDodge Base dodge rating.
     * @param baseArmor Base armor value.
     * @param initialShield Shield points the enemy starts combat with.
     * @param baseRegenPerSecond HP regenerated per second.
     * @param definedSpells A list of (usually 2) spells the enemy can cast.
     */
    public Enemy(String id, String name, int maxHP, int baseAccuracy, int baseDodge,
                 int baseArmor, int initialShield, int baseRegenPerSecond,
                 List<EnemySpellData> definedSpells) {
        this.id = id;
        this.name = name;
        this.maxHP = maxHP;
        this.baseAccuracy = baseAccuracy;
        this.baseDodge = baseDodge;
        this.baseArmor = baseArmor;
        this.initialShield = initialShield;
        this.baseRegenPerSecond = baseRegenPerSecond;
        this.definedSpells = new ArrayList<>(definedSpells); // Use a copy

        if (this.definedSpells != null) {
            this.spellCurrentCooldowns = new double[this.definedSpells.size()];
        } else {
            this.spellCurrentCooldowns = new double[0]; // Should ideally not happen if JSON is valid
        }
        resetCombatState(); // Initialize runtime stats
    }

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
        return maxHP;
    }

    @Override
    public int getCurrentShield() {
        return currentShield;
    }

    // For enemies, "effective" stats are their current stats,
    // as they don't have a complex Stats object or equipment enchantments.
    // Player debuffs will modify these current values directly.
    @Override
    public int getEffectiveArmor() {
        return Math.max(0, currentArmor);
    }

    @Override
    public int getEffectiveDodge() {
        return Math.max(0, currentDodge);
    }

    @Override
    public int getEffectiveAccuracy() {
        return Math.max(0, currentAccuracy);
    }

    @Override
    public int getEffectiveRegenPerSecond() {
        return currentRegenPerSecond; // Can be negative if debuffed
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
        int effectiveHitChance = Math.max(5, Math.min(99, attackerAccuracy - this.currentDodge));
        if (random.nextInt(100) + 1 > effectiveHitChance) {
            System.out.println(this.name + " dodged the attack!"); // Logging
            return;
        }

        // 2. Apply Armor (if not armor piercing)
        int damageAfterArmor = rawDamage;
        if (!isArmorPiercing) {
            damageAfterArmor = Math.max(0, rawDamage - this.currentArmor);
        }

        // 3. Apply Shield (if not shield piercing and shield exists)
        int damageToHP = 0;
        if (isShieldPiercing || this.currentShield <= 0) {
            damageToHP = damageAfterArmor;
        } else {
            int damageAbsorbedByShield = Math.min(damageAfterArmor, this.currentShield);
            this.currentShield -= damageAbsorbedByShield;
            damageToHP = damageAfterArmor - damageAbsorbedByShield;
        }

        // 4. Apply Damage to HP
        this.currentHP -= damageToHP;
        System.out.println(this.name + " took " + damageToHP + " " + (type != null ? type : "") + " damage. HP: " + currentHP + "/" + maxHP + ", Shield: " + currentShield); // Logging
        if (this.currentHP < 0) {
            this.currentHP = 0;
        }
    }

    @Override
    public void applyHealing(int amount) {
        if (!isAlive() || amount <= 0) {
			return;
		}
        this.currentHP = Math.min(this.maxHP, this.currentHP + amount);
        System.out.println(this.name + " healed for " + amount + ". HP: " + currentHP + "/" + maxHP); // Logging
    }

    @Override
    public void applyShield(int amount) {
        if (amount <= 0) {
			return;
		}
        // Enemies might have a max shield defined by their initialShield, or it can grow.
        // For now, let's assume it can grow beyond initial, or initialShield is just the starting amount.
        this.currentShield += amount;
        System.out.println(this.name + " gained " + amount + " shield. Current Shield: " + currentShield);
    }

    // Methods for player debuffs to modify current enemy stats
    @Override
    public void modifyTemporaryArmor(int amountDelta) {
        this.currentArmor = Math.max(0, this.currentArmor + amountDelta);
        System.out.println(this.name + "'s armor changed by " + amountDelta + ". New armor: " + this.currentArmor);
    }

    @Override
    public void modifyTemporaryRegenPerSecond(int amountDelta) {
        this.currentRegenPerSecond += amountDelta;
        System.out.println(this.name + "'s regen changed by " + amountDelta + ". New regen/sec: " + this.currentRegenPerSecond);
    }

    @Override
    public void modifyTemporaryDodge(int amountDelta) {
        this.currentDodge = Math.max(0, this.currentDodge + amountDelta);
        System.out.println(this.name + "'s dodge changed by " + amountDelta + ". New dodge: " + this.currentDodge);
    }

    @Override
    public void modifyTemporaryAccuracy(int amountDelta) {
        this.currentAccuracy += amountDelta; // Accuracy can be negative if that's how hit formula works
        System.out.println(this.name + "'s accuracy changed by " + amountDelta + ". New accuracy: " + this.currentAccuracy);
    }

    @Override
    public void applyPoisonDamagePerSecond(int dpsAmount) {
        this.currentPoisonDpsTaken += dpsAmount;
        if (this.currentPoisonDpsTaken < 0) {
            this.currentPoisonDpsTaken = 0; // Cannot have negative poison
        }
        System.out.println(this.name + " now taking " + this.currentPoisonDpsTaken + " poison DPS.");
    }

    @Override
    public void clearTemporaryCombatEffectsAndPoison() {
        this.currentPoisonDpsTaken = 0;
        // Reset stats modified by debuffs back to their base values
        this.currentAccuracy = this.baseAccuracy;
        this.currentDodge = this.baseDodge;
        this.currentArmor = this.baseArmor;
        this.currentRegenPerSecond = this.baseRegenPerSecond;
    }

    @Override
    public void resetCombatState() {
        this.currentHP = this.maxHP;
        this.currentShield = this.initialShield; // Reset shield to its starting value
        clearTemporaryCombatEffectsAndPoison();

        // Set all spell cooldowns to their maximum
        if (this.definedSpells != null) {
            for (int i = 0; i < spellCurrentCooldowns.length; i++) {
                if (i < this.definedSpells.size() && this.definedSpells.get(i) != null) {
                    spellCurrentCooldowns[i] = this.definedSpells.get(i).getCooldownSeconds();
                }
            }
        }
        System.out.println(this.name + " combat state reset. HP: " + currentHP + "/" + maxHP);
    }

    @Override
    public void updateCooldowns(double deltaTime) {
        if (spellCurrentCooldowns == null) {
			return;
		}
        for (int i = 0; i < spellCurrentCooldowns.length; i++) {
            if (spellCurrentCooldowns[i] > 0) {
                spellCurrentCooldowns[i] -= deltaTime;
                if (spellCurrentCooldowns[i] < 0) {
                    spellCurrentCooldowns[i] = 0;
                }
            }
        }
    }

    @Override
    public List<EnemySpellData> getReadySpells() {
        List<EnemySpellData> readySpells = new ArrayList<>();
        if (definedSpells == null || spellCurrentCooldowns == null) {
			return readySpells;
		}

        for (int i = 0; i < definedSpells.size(); i++) {
            // Ensure index is valid for both lists
            if (i < spellCurrentCooldowns.length && spellCurrentCooldowns[i] <= 0) {
                if (definedSpells.get(i) != null) { // Check if the spell definition itself is not null
                    readySpells.add(definedSpells.get(i));
                }
            }
        }
        return readySpells;
    }

    @Override
    public void triggerCooldownForSpell(EnemySpellData spell) {
        if (definedSpells == null || spellCurrentCooldowns == null || spell == null) {
			return;
		}

        for (int i = 0; i < definedSpells.size(); i++) {
            // Compare by object reference first, then by name if necessary
            if (definedSpells.get(i) == spell || (definedSpells.get(i) != null && definedSpells.get(i).getName().equals(spell.getName()))) {
                if (i < spellCurrentCooldowns.length) {
                    spellCurrentCooldowns[i] = spell.getCooldownSeconds();
                    return;
                }
            }
        }
    }

    @Override
    public void applyPeriodicEffects() {
        if (!isAlive()) {
			return;
		}

        // Net change from regen and poison
        int netHPSChange = this.currentRegenPerSecond - this.currentPoisonDpsTaken;

        if (netHPSChange > 0) { // Net healing
            applyHealing(netHPSChange);
        } else if (netHPSChange < 0) { // Net damage (poison > regen)
            // This damage should bypass shield and armor as it's internal.
            this.currentHP += netHPSChange; // netHPSChange is negative here
            System.out.println(this.name + " took " + (-netHPSChange) + " poison/degen damage. HP: " + currentHP + "/" + maxHP);
            if (this.currentHP < 0) {
                this.currentHP = 0;
            }
        }
        // If netHPSChange is 0, nothing happens from regen/poison this tick.
    }

    // Getters for base stats might be useful for display or specific mechanics
    public String getId() { return id; }
    public int getBaseAccuracy() { return baseAccuracy; }
    public int getBaseDodge() { return baseDodge; }
    public int getBaseArmor() { return baseArmor; }
    public int getBaseRegenPerSecond() { return baseRegenPerSecond; }
    public List<EnemySpellData> getDefinedSpells() { return new ArrayList<>(definedSpells); } // Return a copy
}
