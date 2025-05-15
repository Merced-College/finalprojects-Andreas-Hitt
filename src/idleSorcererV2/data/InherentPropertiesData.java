// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2.data;

public record InherentPropertiesData(
    boolean dealsDoubleDamageToShields,
    boolean ignoresArmor,
    boolean ignoresShield,
    boolean alwaysHits,
    int accuracyBonus,
    String onHitEffect // e.g., "REDUCE_ENEMY_ARMOR_1_FLAT_STACKING", can be null
) {}