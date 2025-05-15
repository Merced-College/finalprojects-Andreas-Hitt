// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import idleSorvererV2.enums.DamageType;

public class EnemySpellData {
    private String name;
    private int damage;
    private double cooldownSeconds;
    private DamageType damageType; // Using the enum directly
    private boolean isArmorPiercing;
    private boolean isShieldPiercing;
    private boolean alwaysHits;
    // Future: could add List<EffectDefinition> appliesEffects;

    // Constructor matching JSON fields
    public EnemySpellData(String name, int damage, double cooldownSeconds, DamageType damageType,
                          boolean isArmorPiercing, boolean isShieldPiercing, boolean alwaysHits) {
        this.name = name;
        this.damage = damage;
        this.cooldownSeconds = cooldownSeconds;
        this.damageType = damageType;
        this.isArmorPiercing = isArmorPiercing; // default to false if not in JSON
        this.isShieldPiercing = isShieldPiercing; // default to false if not in JSON
        this.alwaysHits = alwaysHits;         // default to false if not in JSON
    }

    // Getters
    public String getName() { return name; }
    public int getDamage() { return damage; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public DamageType getDamageType() { return damageType; }
    public boolean isArmorPiercing() { return isArmorPiercing; }
    public boolean isShieldPiercing() { return isShieldPiercing; }
    public boolean isAlwaysHits() { return alwaysHits; }

    @Override
    public String toString() { // For easy printing/logging
        return name + " (DMG:" + damage + " " + damageType + ", CD:" + cooldownSeconds + "s" +
               (isArmorPiercing ? ", AP" : "") +
               (isShieldPiercing ? ", SP" : "") +
               (alwaysHits ? ", Hits" : "") + ")";
    }
}