// Andreas Hitt
// CPSC - 39
// Prof. Kanemoto
// May 13 2025x

package idleSorcererV2.data;

import java.util.ArrayList; // For creating the list in getFullDescription
import java.util.List;

import idleSorvererV2.enums.PrimaryAttributeType;


public record PlayerSpell(
    BaseSpellTemplate baseTemplate,
    double actualCooldownSeconds,    // Randomized cooldown, rounded to 1 decimal
    double effectiveCoreEffectValue, // Base spell's core effect value after its M_spell modifier
    List<EnchantInstance> appliedEnchants, // List of 0-4 enchant instances
    int finalAPCost                  // Calculated AP cost
) {
    /**
     * Convenience getter for the spell's name from its base template.
     * @return The name of the spell.
     */
    public String getName() {
        if (baseTemplate == null) {
			return "Unknown Spell";
		}
        return baseTemplate.name();
    }

    /**
     * Generates a comprehensive description of the spell, including its core effect,
     * inherent properties, and all applied enchantments.
     * @return A formatted string detailing the spell.
     */
    public String getFullDescription() {
        if (baseTemplate == null) {
			return "Invalid Spell Data";
		}

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getName());
        if (baseTemplate.scalingAttribute() != PrimaryAttributeType.NONE) {
            sb.append(" (Scales: ").append(baseTemplate.scalingAttribute()).append(")\n");
        } else {
            sb.append("\n");
        }
        sb.append("  Cooldown: ").append(String.format("%.1f", actualCooldownSeconds)).append("s\n");

        CoreEffectData core = baseTemplate.coreEffect();
        if (core != null) {
            String coreEffectDesc = "  Effect: ";
            // Format effectiveCoreEffectValue to one decimal place if it's not a whole number,
            // or as an integer if it is.
            String formattedCoreValue = String.format(effectiveCoreEffectValue == (long) effectiveCoreEffectValue ? "%.0f" : "%.1f", effectiveCoreEffectValue);

            switch (core.type()) {
                case DAMAGE:
                    coreEffectDesc += formattedCoreValue + " " + core.damageType() + " Damage";
                    break;
                case HEALING:
                    coreEffectDesc += formattedCoreValue + " " + core.healingType() + " Healing";
                    break;
                case SHIELD_APPLICATION:
                    coreEffectDesc += formattedCoreValue + " Shield";
                    break;
                case APPLY_DOT:
                    coreEffectDesc += "Applies " + formattedCoreValue + " " + core.dotType() + "/sec per stack";
                    if (core.dealsNoInitialDamage()) {
						coreEffectDesc += " (No initial damage)";
					}
                    if (core.stacking()) {
						coreEffectDesc += " (Stacking)";
					}
                    break;
                case BUFF_PLAYER:
                case DEBUFF_ENEMY:
                default:
                    coreEffectDesc += formattedCoreValue + " " + core.type().toString().toLowerCase().replace("_", " ");
                    break;
            }
            sb.append(coreEffectDesc).append("\n");
        }

        InherentPropertiesData props = baseTemplate.inherentProperties();
        if (props != null) {
            List<String> inherentDescs = new ArrayList<>();
            if (props.accuracyBonus() > 0) {
				inherentDescs.add("+" + props.accuracyBonus() + " Accuracy");
			}
            if (props.alwaysHits()) {
				inherentDescs.add("Always Hits");
			}
            if (props.ignoresArmor()) {
				inherentDescs.add("Ignores Armor");
			}
            if (props.ignoresShield()) {
				inherentDescs.add("Ignores Shield");
			}
            if (props.dealsDoubleDamageToShields()) {
				inherentDescs.add("Double Damage to Shields");
			}
            if (props.onHitEffect() != null && !props.onHitEffect().isEmpty()) {
                inherentDescs.add("On-Hit: " + props.onHitEffect().replace("_", " ").toLowerCase());
            }

            if (!inherentDescs.isEmpty()) {
                sb.append("  Inherent: ").append(String.join(", ", inherentDescs)).append("\n");
            }
        }

        if (appliedEnchants != null && !appliedEnchants.isEmpty()) {
            sb.append("  Enchantments (").append(appliedEnchants.size()).append("):\n");
            for (EnchantInstance enchant : appliedEnchants) {
                if (enchant != null) { // Basic null check for safety
                    sb.append("    - ").append(enchant.getDescription()).append("\n");
                }
            }
        }
        sb.append("  AP Value: ").append(finalAPCost);
        return sb.toString();
    }
}
