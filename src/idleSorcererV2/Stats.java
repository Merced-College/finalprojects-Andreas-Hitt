// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2;

import java.util.EnumMap;
import java.util.Map;

import idleSorvererV2.enums.DamageType;
import idleSorvererV2.enums.HealingType;
import idleSorvererV2.enums.PrimaryAttributeType;

/**
 * Holds all primary attributes and calculates derived secondary stats for an entity.
 * Primary attributes are intended to be set externally (e.g., by player spending points
 * Secondary stats are calculated internally based on primary attributes.
 * Effects (buffs/debuffs) will modify these values further at the Entity level.
 */
public class Stats {

    // Primary Attributes
    private int agility;
    private int charm;
    private int cunning;
    private int dexterity;
    private int fortitude;
    private int intellect;
    private int malice;
    private int mind;
    private int piety;
    private int wisdom;

    // For tracking total points and derived flat HP bonus (for +2 HP per attribute point)
    private int totalPrimaryAttributePoints;
    private double flatHpBonusFromTotalAttributes;

    // Calculated Secondary Stats
    // These store the values derived from primary attributes and base values.
    // Temporary effects (buffs/debuffs) will be handled at the Entity level.

    // Defensive / Utility
    private double calculatedDodgePercent;             // Base 0% + 1% per Agility
    private double calculatedAccuracyPercent;          // Base 100% + 1% per Dexterity
    private double calculatedCastingHastePercent;      // Base 0% + 1% per Cunning
    private double calculatedMaxHealthBonusPercent;    // Base 0% + 5% per Fortitude
    private double calculatedDebuffEffectivenessPercent; // Base 0% + 1% per Malice
    private double calculatedShieldEffectivenessPercent; // Base 0% + 10% per Mind

    // Damage And Healing Bonuses (as percentage increase, e.g., 10.0 means +10%)
    private Map<DamageType, Double> calculatedDamageBonuses;
    private Map<HealingType, Double> calculatedHealingBonuses;

    // Other stats often modified by effects, base values are 0
    private double poisonPerSecond;
    private double regenPerSecond;
    private double armor;
    private double shieldValue;

    // Constants for base values
    private static final double BASE_DODGE_PERCENT = 0.0;
    private static final double BASE_ACCURACY_PERCENT = 100.0;
    private static final double BASE_CASTING_HASTE_PERCENT = 0.0;
    private static final double BASE_MAX_HEALTH_BONUS_PERCENT = 0.0;
    private static final double BASE_DEBUFF_EFFECTIVENESS_PERCENT = 0.0;
    private static final double BASE_SHIELD_EFFECTIVENESS_PERCENT = 0.0;

    /**
     * Constructor. Initializes primary attributes to 0 and calculates initial secondary stats.
     */
    public Stats() {
        calculatedDamageBonuses = new EnumMap<>(DamageType.class);
        calculatedHealingBonuses = new EnumMap<>(HealingType.class);

        this.agility = 0;
        this.charm = 0;
        this.cunning = 0;
        this.dexterity = 0;
        this.fortitude = 0;
        this.intellect = 0;
        this.malice = 0;
        this.mind = 0;
        this.piety = 0;
        this.wisdom = 0;

        this.totalPrimaryAttributePoints = 0;
        this.flatHpBonusFromTotalAttributes = 0.0;

        this.poisonPerSecond = 0.0;
        this.regenPerSecond = 0.0;
        this.armor = 0.0;
        this.shieldValue = 0.0;

        recalculateSecondaryStats();
    }

    /**
     * Recalculates all derived secondary stats based on current primary attribute values.
     * Should be called whenever a primary attribute is changed.
     */
    private void recalculateSecondaryStats() {
        // Calculate total primary attribute points and derived flat HP bonus
        this.totalPrimaryAttributePoints = this.agility + this.charm + this.cunning +
                                           this.dexterity + this.fortitude + this.intellect +
                                           this.malice + this.mind + this.piety + this.wisdom;
        this.flatHpBonusFromTotalAttributes = this.totalPrimaryAttributePoints * 2.0; // +2 HP per point

        // Calculate Utility/Defensive Stats (using the declared constants)
        calculatedDodgePercent = BASE_DODGE_PERCENT + (this.agility * 1.0);
        calculatedAccuracyPercent = BASE_ACCURACY_PERCENT + (this.dexterity * 1.0);
        calculatedCastingHastePercent = BASE_CASTING_HASTE_PERCENT + (this.cunning * 1.0);
        calculatedMaxHealthBonusPercent = BASE_MAX_HEALTH_BONUS_PERCENT + (this.fortitude * 5.0);
        calculatedDebuffEffectivenessPercent = BASE_DEBUFF_EFFECTIVENESS_PERCENT + (this.malice * 1.0);
        calculatedShieldEffectivenessPercent = BASE_SHIELD_EFFECTIVENESS_PERCENT + (this.mind * 10.0);

        // Calculate Damage Bonuses
        calculatedDamageBonuses.clear();
        calculatedDamageBonuses.put(DamageType.ARCANE, this.agility * 10.0);
        calculatedDamageBonuses.put(DamageType.SONIC, this.charm * 10.0);
        calculatedDamageBonuses.put(DamageType.ENTROPY, this.cunning * 10.0);
        calculatedDamageBonuses.put(DamageType.ASTRAL, this.dexterity * 10.0);
        calculatedDamageBonuses.put(DamageType.ACID, this.fortitude * 10.0);
        calculatedDamageBonuses.put(DamageType.FIRE, this.intellect * 10.0);
        calculatedDamageBonuses.put(DamageType.COLD, this.intellect * 10.0);
        calculatedDamageBonuses.put(DamageType.DARK, this.malice * 10.0);
        calculatedDamageBonuses.put(DamageType.PSYCHIC, this.mind * 10.0);
        calculatedDamageBonuses.put(DamageType.HOLY, this.piety * 10.0);
        calculatedDamageBonuses.put(DamageType.LIGHTNING, this.wisdom * 10.0);
        // If DamageType.GENERAL or .PHYSICAL is in your enum, and no primary attribute
        // above directly boosts it, the loop below will ensure it gets a 0.0 default.
        // Example: calculatedDamageBonuses.put(DamageType.GENERAL, 0.0); // If it had no direct attribute source

        for (DamageType type : DamageType.values()) {
            calculatedDamageBonuses.putIfAbsent(type, 0.0);
        }

        // Calculate Healing Bonuses
        calculatedHealingBonuses.clear();
        calculatedHealingBonuses.put(HealingType.CHARM_HEALING, this.charm * 10.0);
        calculatedHealingBonuses.put(HealingType.PIETY_HEALING, this.piety * 10.0);
        calculatedHealingBonuses.put(HealingType.WISDOM_HEALING, this.wisdom * 10.0);

        for (HealingType type : HealingType.values()) {
            calculatedHealingBonuses.putIfAbsent(type, 0.0);
        }
    }

    // Getters for Primary Attributes
    public int getAgility() { return agility; }
    public int getCharm() { return charm; }
    public int getCunning() { return cunning; }
    public int getDexterity() { return dexterity; }
    public int getFortitude() { return fortitude; }
    public int getIntellect() { return intellect; }
    public int getMalice() { return malice; }
    public int getMind() { return mind; }
    public int getPiety() { return piety; }
    public int getWisdom() { return wisdom; }

    /**
     * Gets the value of a specific primary attribute using its enum type.
     * This is useful for generic access, like calculating AP costs in the Player class.
     * @param type The PrimaryAttributeType enum of the attribute to retrieve.
     * @return The current integer value of the specified primary attribute. Returns 0 if type is NONE or unrecognized.
     */
    public int getPrimaryAttributeValue(PrimaryAttributeType type) {
        if (type == null) {
			return 0;
		}
        switch (type) {
            case AGILITY: return getAgility();
            case CHARM: return getCharm();
            case CUNNING: return getCunning();
            case DEXTERITY: return getDexterity();
            case FORTITUDE: return getFortitude();
            case INTELLECT: return getIntellect();
            case MALICE: return getMalice();
            case MIND: return getMind();
            case PIETY: return getPiety();
            case WISDOM: return getWisdom();
            case NONE: // Fallthrough
            default:
                // System.err.println("Stats Warning: Requested value for unhandled PrimaryAttributeType: " + type);
                return 0; // Or throw an IllegalArgumentException if NONE is not expected here
        }
    }

    // Setters for Primary Attributes (trigger recalculation)
    public void setAgility(int agility) { this.agility = Math.max(0, agility); recalculateSecondaryStats(); }
    public void setCharm(int charm) { this.charm = Math.max(0, charm); recalculateSecondaryStats(); }
    public void setCunning(int cunning) { this.cunning = Math.max(0, cunning); recalculateSecondaryStats(); }
    public void setDexterity(int dexterity) { this.dexterity = Math.max(0, dexterity); recalculateSecondaryStats(); }
    public void setFortitude(int fortitude) { this.fortitude = Math.max(0, fortitude); recalculateSecondaryStats(); }
    public void setIntellect(int intellect) { this.intellect = Math.max(0, intellect); recalculateSecondaryStats(); }
    public void setMalice(int malice) { this.malice = Math.max(0, malice); recalculateSecondaryStats(); }
    public void setMind(int mind) { this.mind = Math.max(0, mind); recalculateSecondaryStats(); }
    public void setPiety(int piety) { this.piety = Math.max(0, piety); recalculateSecondaryStats(); }
    public void setWisdom(int wisdom) { this.wisdom = Math.max(0, wisdom); recalculateSecondaryStats(); }

    // Getters for Calculated Secondary Stats
    public double getFlatHpBonusFromTotalAttributes() {
        return flatHpBonusFromTotalAttributes;
    }
    public int getTotalPrimaryAttributePoints() {
        return totalPrimaryAttributePoints;
    }

    public double getCalculatedDodgePercent() { return calculatedDodgePercent; }
    public double getCalculatedAccuracyPercent() { return calculatedAccuracyPercent; }
    public double getCalculatedCastingHastePercent() { return calculatedCastingHastePercent; }
    public double getCalculatedMaxHealthBonusPercent() { return calculatedMaxHealthBonusPercent; }
    public double getCalculatedDebuffEffectivenessPercent() { return calculatedDebuffEffectivenessPercent; }
    public double getCalculatedShieldEffectivenessPercent() { return calculatedShieldEffectivenessPercent; }

    /**
     * Gets the calculated damage bonus percentage for a specific damage type,
     * based solely on primary attributes for that type.
     * @param queryType The DamageType.
     * @return The bonus percentage (e.g., 10.0 for +10%). Returns 0.0 if type not found or not boosted.
     */
    public double getCalculatedDamageBonusPercent(DamageType queryType) {
        // Simply return the bonus stored for this specific type.
        // Any "GENERAL" or "PHYSICAL" type will just be treated as another specific type
        // getting whatever bonus is directly calculated for it (which is 0 if no primary attribute boosts it).
        return calculatedDamageBonuses.getOrDefault(queryType, 0.0);
    }

    /**
     * Gets the calculated healing bonus percentage for a specific healing type.
     * @param type The HealingType.
     * @return The bonus percentage (e.g., 10.0 for +10%). Returns 0.0 if type not found.
     */
    public double getCalculatedHealingBonusPercent(HealingType type) {
         return calculatedHealingBonuses.getOrDefault(type, 0.0);
    }

    // Getters/Setters for Effect-Modified Stats
    public double getPoisonPerSecond() { return poisonPerSecond; }
    public void setPoisonPerSecond(double poisonPerSecond) { this.poisonPerSecond = poisonPerSecond; }

    public double getRegenPerSecond() { return regenPerSecond; }
    public void setRegenPerSecond(double regenPerSecond) { this.regenPerSecond = regenPerSecond; }

    public double getArmor() { return armor; }
    public void setArmor(double armor) { this.armor = armor; }

    public double getShieldValue() { return shieldValue; }
    public void setShieldValue(double shieldValue) { this.shieldValue = Math.max(0, shieldValue); }

    /**
     * Utility method to add points to a primary attribute by its name.
     * This method is used by the Player class when spending AP.
     * @param attributeName The name of the attribute (case-insensitive).
     * @param points The number of attribute skill points to add (should be positive, usually 1).
     */
    public boolean addPointsToAttribute(String attributeName, int points) {
        if (points <= 0) {
            System.err.println("Stats Error: Points to add must be positive. Received: " + points);
            return false; // Indicate failure
        }

        PrimaryAttributeType typeToUpgrade = null;
        try {
            typeToUpgrade = PrimaryAttributeType.valueOf(attributeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Stats Warning: Unknown attribute name '" + attributeName + "' in addPointsToAttribute");
            return false; // Indicate failure
        }

        if (typeToUpgrade == PrimaryAttributeType.NONE) {
             System.err.println("Stats Error: Cannot add points to NONE attribute type.");
            return false;
        }

        switch (typeToUpgrade) {
            case AGILITY:   setAgility(this.agility + points); break;
            case CHARM:     setCharm(this.charm + points); break;
            case CUNNING:   setCunning(this.cunning + points); break;
            case DEXTERITY: setDexterity(this.dexterity + points); break;
            case FORTITUDE: setFortitude(this.fortitude + points); break;
            case INTELLECT: setIntellect(this.intellect + points); break;
            case MALICE:    setMalice(this.malice + points); break;
            case MIND:      setMind(this.mind + points); break;
            case PIETY:     setPiety(this.piety + points); break;
            case WISDOM:    setWisdom(this.wisdom + points); break;
            default: // Should be caught by valueOf or NONE check, but as a safeguard
                return false;
        }
        return true; // Indicate success
    }
}