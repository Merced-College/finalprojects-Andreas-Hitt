// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.util.List;

import idleSorvererV2.enums.DamageType;

/**
 * Interface for any entity that can participate in combat.
 * @param <T_SPELL> The type of spell object this combatant uses.
 */
public interface Combatant<T_SPELL> {
    String getName();
    boolean isAlive();

    int getCurrentHP();
    int getMaxHP();
    int getCurrentShield();

    // Methods to get effective combat stats (after all base stats, enchants, temporary effects)
    int getEffectiveArmor();
    int getEffectiveDodge();
    int getEffectiveAccuracy();
    int getEffectiveRegenPerSecond();
    int getCurrentPoisonTakenPerSecond();

    /**
     * Applies damage to the combatant.
     * @param rawDamage The initial damage amount before any reductions.
     * @param type The DamageType of the incoming damage.
     * @param isArmorPiercing Whether the damage ignores armor.
     * @param isShieldPiercing Whether the damage ignores shield.
     * @param attackerAccuracy The accuracy of the attacker, used for dodge calculation.
     */
    void takeDamage(int rawDamage, DamageType type, boolean isArmorPiercing, boolean isShieldPiercing, int attackerAccuracy);

    void applyHealing(int amount);
    void applyShield(int amount); // Method to add shield points

    // Methods for temporary stat modifications from combat effects (buffs/debuffs)
    void modifyTemporaryArmor(int amountDelta); // Positive to add, negative to remove
    void modifyTemporaryRegenPerSecond(int amountDelta);
    void modifyTemporaryDodge(int amountDelta);
    void modifyTemporaryAccuracy(int amountDelta);

    void applyPoisonDamagePerSecond(int dpsAmount); // Adds to existing poison
    void clearTemporaryCombatEffectsAndPoison(); // Clears poison and temporary combat buffs/debuffs

    void resetCombatState(); // Resets HP, shield, cooldowns, temporary effects, poison
    void updateCooldowns(double deltaTime); // For active spells

    List<T_SPELL> getReadySpells(); // Gets spells that are off cooldown
    void triggerCooldownForSpell(T_SPELL spell); // Puts the specified spell on its full cooldown

    void applyPeriodicEffects(); // For combatant's own regen and poison ticks at the end of a "second"
}
