// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025

package idleSorcererV2.data;

import idleSorvererV2.enums.CoreEffectType;
import idleSorvererV2.enums.DamageType;
import idleSorvererV2.enums.HealingType;

public record CoreEffectData(
    CoreEffectType type,
    double baseValue,
    DamageType damageType,    // Can be null if effect type is not DAMAGE
    HealingType healingType,  // Can be null if effect type is not HEALING
    String dotType,           // e.g., "ENTROPY_POISON", can be null
    boolean dealsNoInitialDamage,
    boolean stacking
) {}